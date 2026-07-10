package com.ren42377.bimalaunch.practice

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

internal object PracticeDatabase {
    fun ensureDatabase(root: File): File {
        val dbFile = File(root, "data/db.json")
        if (dbFile.exists()) return dbFile
        dbFile.parentFile?.mkdirs()
        val db = seedDatabase()
        writeJson(dbFile, db)
        return dbFile
    }

    fun loadDb(root: File): JSONObject {
        val dbFile = ensureDatabase(root)
        return runCatching { JSONObject(dbFile.readText(Charsets.UTF_8)) }.getOrElse { seedDatabase() }
    }

    fun saveDb(root: File, db: JSONObject) {
        db.optJSONObject("meta")?.put("dateTime", now())
        writeJson(File(root, "data/db.json"), db)
    }

    fun seedDatabase(): JSONObject {
        val db = JSONObject()
        db.put("meta", JSONObject().put("dateTime", now()))
        db.put("admin", JSONObject().put("username", PracticeConfig.DEFAULT_ADMIN_USERNAME).put("password", PracticeConfig.DEFAULT_ADMIN_PASSWORD))
        db.put("pengawas", JSONArray())
        db.put("soal", JSONObject())
        db.put("tree", JSONObject())
        db.put("broadcast", JSONObject().put("message", "").put("updatedAt", ""))
        return db
    }

    fun collectionNode(db: JSONObject, path: List<String>, create: Boolean): JSONObject? {
        var node = db.optJSONObject("tree") ?: JSONObject().also { db.put("tree", it) }
        for (part in path) {
            var next = node.optJSONObject(part)
            if (next == null) {
                if (!create) return null
                next = JSONObject()
                node.put(part, next)
            }
            node = next
        }
        return node
    }

    fun isDocument(value: JSONObject): Boolean {
        val keys = value.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (value.opt(key) !is JSONObject) return true
        }
        return false
    }

    fun readCollectionOrDoc(db: JSONObject, path: List<String>): List<JSONObject> {
        val parts = path.toMutableList()
        if (parts.isEmpty()) return emptyList()
        val id = parts.removeAt(parts.size - 1)
        val parent = collectionNode(db, parts, false) ?: return emptyList()
        val node = parent.optJSONObject(id) ?: return emptyList()
        return if (isDocument(node)) {
            listOf(cloneObject(node))
        } else {
            val keys = node.keys()
            val result = mutableListOf<JSONObject>()
            while (keys.hasNext()) {
                val key = keys.next()
                val child = node.opt(key)
                if (child is JSONObject && isDocument(child)) {
                    result.add(cloneObject(child))
                }
            }
            result
        }
    }

    fun setDoc(db: JSONObject, path: List<String>, data: JSONObject, merge: Boolean): JSONObject {
        val parts = path.toMutableList()
        val id = parts.removeAt(parts.size - 1)
        val parent = collectionNode(db, parts, true) ?: JSONObject()
        val existing = parent.optJSONObject(id)
        val next = if (merge && existing != null) cloneObject(existing) else JSONObject()
        mergeInto(next, data)
        if (!next.has("id")) next.put("id", id)
        parent.put(id, next)
        return next
    }

    fun deleteDoc(db: JSONObject, path: List<String>): Boolean {
        val parts = path.toMutableList()
        val id = parts.removeAt(parts.size - 1)
        val parent = collectionNode(db, parts, false) ?: return false
        val existed = parent.has(id)
        parent.remove(id)
        return existed
    }

    fun mergeInto(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            target.put(key, source.opt(key))
        }
    }

    fun cloneObject(source: JSONObject): JSONObject = JSONObject(source.toString())

    fun now(): String = Instant.now().toString()

    private fun writeJson(file: File, json: JSONObject) {
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2), Charsets.UTF_8)
    }
}
