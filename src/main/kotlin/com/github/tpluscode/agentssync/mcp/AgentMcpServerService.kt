package com.github.tpluscode.agentssync.mcp

import com.github.tpluscode.agentssync.model.AgentHistorySnapshot
import com.github.tpluscode.agentssync.service.AgentSyncSettingsService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * Embedded Model Context Protocol (MCP) server service.
 * Allows AI Assistant, AIR, Junie, Claude, and external LLM agents to read and modify AGENTS.md.
 */
@Service(Service.Level.APP)
class AgentMcpServerService {

    private val LOG = Logger.getInstance(AgentMcpServerService::class.java)
    private var server: HttpServer? = null
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        fun getInstance(): AgentMcpServerService {
            return ApplicationManager.getApplication().getService(AgentMcpServerService::class.java)
        }
    }

    /**
     * Starts the MCP server if enabled in settings.
     */
    @Synchronized
    fun start() {
        val settings = AgentSyncSettingsService.getInstance().state
        if (!settings.mcpEnabled) {
            LOG.info("Agent MCP Server is disabled in settings.")
            return
        }

        stop()

        try {
            val port = settings.mcpPort
            val address = InetSocketAddress("127.0.0.1", port)
            val newServer = HttpServer.create(address, 0)

            newServer.createContext("/", McpHttpHandler(gson))
            newServer.executor = Executors.newCachedThreadPool()
            newServer.start()
            server = newServer
            LOG.info("Agent MCP Server started on http://127.0.0.1:$port/")
        } catch (e: Exception) {
            LOG.error("Failed to start Agent MCP Server on port ${settings.mcpPort}", e)
        }
    }

    /**
     * Stops the running MCP server.
     */
    @Synchronized
    fun stop() {
        server?.let {
            try {
                it.stop(0)
                LOG.info("Agent MCP Server stopped.")
            } catch (e: Exception) {
                LOG.warn("Error stopping Agent MCP Server", e)
            } finally {
                server = null
            }
        }
    }

    /**
     * Restarts the MCP server with updated configuration.
     */
    fun restart() {
        stop()
        start()
    }

    fun isRunning(): Boolean = server != null

    private class McpHttpHandler(private val gson: Gson) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                // Set CORS headers
                exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
                exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, Authorization")
                exchange.responseHeaders.add("Content-Type", "application/json; charset=UTF-8")

                if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                    exchange.sendResponseHeaders(204, -1)
                    return
                }

                val path = exchange.requestURI.path

                if (exchange.requestMethod.equals("GET", ignoreCase = true)) {
                    when (path) {
                        "/", "/health" -> {
                            val resp = JsonObject().apply {
                                addProperty("status", "ok")
                                addProperty("service", "Agents Sync MCP Server")
                                addProperty("version", "1.0.0")
                            }
                            sendJsonResponse(exchange, 200, resp)
                            return
                        }
                        "/content" -> {
                            val resp = JsonObject().apply {
                                addProperty("content", AgentSyncSettingsService.getInstance().getContent())
                            }
                            sendJsonResponse(exchange, 200, resp)
                            return
                        }
                    }
                }

                if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    val reader = InputStreamReader(exchange.requestBody, StandardCharsets.UTF_8)
                    val requestJson = JsonParser.parseReader(reader).asJsonObject
                    val responseJson = processMcpRequest(requestJson)
                    sendJsonResponse(exchange, 200, responseJson)
                    return
                }

                // Default 404
                sendJsonResponse(exchange, 404, JsonObject().apply { addProperty("error", "Not Found") })
            } catch (e: Exception) {
                val errorObj = JsonObject().apply {
                    addProperty("error", e.message ?: "Internal Server Error")
                }
                sendJsonResponse(exchange, 500, errorObj)
            }
        }

        private fun processMcpRequest(request: JsonObject): JsonObject {
            val id = if (request.has("id")) request.get("id") else null
            val method = if (request.has("method")) request.get("method").asString else ""
            val params = if (request.has("params") && request.get("params").isJsonObject) request.getAsJsonObject("params") else JsonObject()

            val response = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                if (id != null) add("id", id)
            }

            when (method) {
                "initialize" -> {
                    val result = JsonObject().apply {
                        addProperty("protocolVersion", "2024-11-05")
                        val capabilities = JsonObject().apply {
                            val tools = JsonObject()
                            val resources = JsonObject()
                            val prompts = JsonObject()
                            add("tools", tools)
                            add("resources", resources)
                            add("prompts", prompts)
                        }
                        add("capabilities", capabilities)
                        val serverInfo = JsonObject().apply {
                            addProperty("name", "intellij-agents-sync")
                            addProperty("version", "1.0.0")
                        }
                        add("serverInfo", serverInfo)
                    }
                    response.add("result", result)
                }

                "tools/list" -> {
                    val toolsArray = JsonArray().apply {
                        add(buildTool(
                            "read_agents_md",
                            "Reads the current synchronized system-wide AGENTS.md file content and metadata",
                            JsonObject()
                        ))
                        add(buildTool(
                            "update_agents_md",
                            "Overwrites the system-wide AGENTS.md content and syncs across projects/machines",
                            JsonObject().apply {
                                addProperty("type", "object")
                                val properties = JsonObject().apply {
                                    val contentProp = JsonObject().apply {
                                        addProperty("type", "string")
                                        addProperty("description", "Complete new Markdown content for AGENTS.md")
                                    }
                                    val messageProp = JsonObject().apply {
                                        addProperty("type", "string")
                                        addProperty("description", "Commit message explaining what was changed and why")
                                    }
                                    add("content", contentProp)
                                    add("message", messageProp)
                                }
                                add("properties", properties)
                                val required = JsonArray().apply { add("content") }
                                add("required", required)
                            }
                        ))
                        add(buildTool(
                            "append_agents_rule",
                            "Appends a new instruction, guideline, or section to the system-wide AGENTS.md file",
                            JsonObject().apply {
                                addProperty("type", "object")
                                val properties = JsonObject().apply {
                                    val ruleProp = JsonObject().apply {
                                        addProperty("type", "string")
                                        addProperty("description", "Guideline text or section to append")
                                    }
                                    val messageProp = JsonObject().apply {
                                        addProperty("type", "string")
                                        addProperty("description", "Summary message of the rule being added")
                                    }
                                    add("rule", ruleProp)
                                    add("message", messageProp)
                                }
                                add("properties", properties)
                                val required = JsonArray().apply { add("rule") }
                                add("required", required)
                            }
                        ))
                        add(buildTool(
                            "get_agents_history",
                            "Retrieves snapshot history list of previous AGENTS.md revisions",
                            JsonObject()
                        ))
                        add(buildTool(
                            "rollback_agents_md",
                            "Rolls back AGENTS.md to a historical snapshot revision by ID",
                            JsonObject().apply {
                                addProperty("type", "object")
                                val properties = JsonObject().apply {
                                    val idProp = JsonObject().apply {
                                        addProperty("type", "string")
                                        addProperty("description", "Snapshot ID to roll back to")
                                    }
                                    add("snapshot_id", idProp)
                                }
                                add("properties", properties)
                                val required = JsonArray().apply { add("snapshot_id") }
                                add("required", required)
                            }
                        ))
                    }
                    val result = JsonObject().apply { add("tools", toolsArray) }
                    response.add("result", result)
                }

                "tools/call" -> {
                    val toolName = if (params.has("name")) params.get("name").asString else ""
                    val arguments = if (params.has("arguments") && params.get("arguments").isJsonObject) params.getAsJsonObject("arguments") else JsonObject()

                    val toolResult = handleToolCall(toolName, arguments)
                    response.add("result", toolResult)
                }

                "resources/list" -> {
                    val resourcesArray = JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("uri", "agent://system/AGENTS.md")
                            addProperty("name", "System AGENTS.md")
                            addProperty("mimeType", "text/markdown")
                            addProperty("description", "Synchronized system-wide AGENTS.md guidelines")
                        })
                    }
                    val result = JsonObject().apply { add("resources", resourcesArray) }
                    response.add("result", result)
                }

                "resources/read" -> {
                    val content = AgentSyncSettingsService.getInstance().getContent()
                    val contentsArray = JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("uri", "agent://system/AGENTS.md")
                            addProperty("mimeType", "text/markdown")
                            addProperty("text", content)
                        })
                    }
                    val result = JsonObject().apply { add("contents", contentsArray) }
                    response.add("result", result)
                }

                else -> {
                    val errorObj = JsonObject().apply {
                        addProperty("code", -32601)
                        addProperty("message", "Method not found: $method")
                    }
                    response.add("error", errorObj)
                }
            }

            return response
        }

        private fun buildTool(name: String, description: String, schema: JsonObject): JsonObject {
            return JsonObject().apply {
                addProperty("name", name)
                addProperty("description", description)
                add("inputSchema", schema)
            }
        }

        private fun handleToolCall(name: String, args: JsonObject): JsonObject {
            val service = AgentSyncSettingsService.getInstance()
            val textContent: String

            when (name) {
                "read_agents_md" -> {
                    val content = service.getContent()
                    val history = service.getHistory()
                    val lastSnapshot = history.firstOrNull()
                    textContent = "## System AGENTS.md Content\n\n$content\n\n---\nLast updated by: ${lastSnapshot?.author ?: "Unknown"} at ${lastSnapshot?.timestamp ?: 0}\nSnapshots in history: ${history.size}"
                }

                "update_agents_md" -> {
                    val newContent = if (args.has("content")) args.get("content").asString else ""
                    val message = if (args.has("message")) args.get("message").asString else "Updated by AI Assistant"
                    val success = service.updateContent(newContent, "AI Assistant", message)
                    textContent = if (success) {
                        "Successfully updated AGENTS.md ($message). Change saved and synced."
                    } else {
                        "AGENTS.md content was identical; no change needed."
                    }
                }

                "append_agents_rule" -> {
                    val rule = if (args.has("rule")) args.get("rule").asString else ""
                    val message = if (args.has("message")) args.get("message").asString else "Appended rule by AI Assistant"
                    if (rule.isBlank()) {
                        textContent = "Error: rule cannot be empty."
                    } else {
                        service.appendRule(rule, "AI Assistant", message)
                        textContent = "Successfully appended guideline rule to AGENTS.md ($message)."
                    }
                }

                "get_agents_history" -> {
                    val history = service.getHistory()
                    val summaryList = history.map {
                        mapOf(
                            "id" to it.id,
                            "timestamp" to it.timestamp,
                            "author" to it.author,
                            "message" to it.message,
                            "contentLength" to it.content.length
                        )
                    }
                    textContent = gson.toJson(summaryList)
                }

                "rollback_agents_md" -> {
                    val snapshotId = if (args.has("snapshot_id")) args.get("snapshot_id").asString else ""
                    val success = service.rollbackToSnapshot(snapshotId, "AI Assistant Rollback")
                    textContent = if (success) {
                        "Successfully rolled back AGENTS.md to snapshot $snapshotId."
                    } else {
                        "Failed to roll back: snapshot $snapshotId not found."
                    }
                }

                else -> {
                    textContent = "Unknown tool: $name"
                }
            }

            return JsonObject().apply {
                val contentArray = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", textContent)
                    })
                }
                add("content", contentArray)
            }
        }

        private fun sendJsonResponse(exchange: HttpExchange, statusCode: Int, json: JsonObject) {
            val responseBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(statusCode, responseBytes.size.toLong())
            exchange.responseBody.use { os ->
                os.write(responseBytes)
            }
        }
    }
}
