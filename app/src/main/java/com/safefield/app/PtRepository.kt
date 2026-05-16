package com.safefield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class PtRepository(context: Context) {
    private val prefs = context.getSharedPreferences("safefield_pt", Context.MODE_PRIVATE)

    fun load(): PtData {
        val json = prefs.getString("draft_json", null) ?: return PtData().also { it.signatureB64 = prefs.getString("signature_b64", "").orEmpty() }
        return runCatching {
            val o = JSONObject(json)
            PtData(
                company = o.optString("company"), area = o.optString("area"), place = o.optString("place"), responsible = o.optString("responsible"),
                startMillis = o.optLong("startMillis", System.currentTimeMillis()), validityHours = o.optInt("validityHours", 8),
                endMillis = o.optLong("endMillis", System.currentTimeMillis() + 8L * 60L * 60L * 1000L), teamName = o.optString("teamName"),
                description = o.optString("description"), tools = o.optString("tools"), products = o.optString("products"),
                emergencyPoint = o.optString("emergencyPoint"), emergencyPhone = o.optString("emergencyPhone"), emergencyProcedure = o.optString("emergencyProcedure"), observations = o.optString("observations"),
                manualActivity = o.optString("manualActivity"),
                activities = o.optJSONArray("activities").toStringSet(), checklist = o.optJSONObject("checklist").toStringMap(), controls = o.optJSONObject("controls").toStringMap(),
                workers = o.optJSONArray("workers").toWorkers(), photoUris = o.optJSONArray("photoUris").toStringList(),
                signatureB64 = prefs.getString("signature_b64", o.optString("signatureB64")).orEmpty(), history = o.optJSONArray("history").toHistory()
            )
        }.getOrElse { PtData() }
    }

    fun save(data: PtData) {
        prefs.edit().putString("draft_json", toJson(data).toString()).putString("signature_b64", data.signatureB64).apply()
    }

    fun clear() { prefs.edit().clear().apply() }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(value: String): Bitmap? {
        if (value.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun toJson(data: PtData): JSONObject = JSONObject().apply {
        put("company", data.company); put("area", data.area); put("place", data.place); put("responsible", data.responsible)
        put("startMillis", data.startMillis); put("validityHours", data.validityHours); put("endMillis", data.endMillis)
        put("teamName", data.teamName); put("description", data.description); put("tools", data.tools); put("products", data.products)
        put("emergencyPoint", data.emergencyPoint); put("emergencyPhone", data.emergencyPhone); put("emergencyProcedure", data.emergencyProcedure); put("observations", data.observations)
        put("manualActivity", data.manualActivity)
        put("activities", JSONArray(data.activities.toList())); put("checklist", JSONObject(data.checklist.toMap())); put("controls", JSONObject(data.controls.toMap()))
        put("photoUris", JSONArray(data.photoUris)); put("signatureB64", data.signatureB64)
        put("workers", JSONArray().also { arr -> data.workers.forEach { arr.put(JSONObject().put("name", it.name).put("role", it.role).put("signatureB64", it.signatureB64).put("signedAt", it.signedAt)) } })
        put("history", JSONArray().also { arr ->
            data.history.forEach {
                arr.put(
                    JSONObject()
                        .put("ptNumber", it.ptNumber)
                        .put("emittedAt", it.emittedAt)
                        .put("place", it.place)
                        .put("company", it.company)
                        .put("responsible", it.responsible)
                        .put("fileName", it.fileName)
                        .put("status", it.status)
                        .put("startMillis", it.startMillis)
                        .put("endMillis", it.endMillis)
                        .put("closedAt", it.closedAt)
                        .put("closeNote", it.closeNote)
                )
            }
        })
    }

    private fun JSONArray?.toStringSet(): MutableSet<String> { val set = linkedSetOf<String>(); if (this != null) for (i in 0 until length()) set.add(optString(i)); return set }
    private fun JSONArray?.toStringList(): MutableList<String> { val list = mutableListOf<String>(); if (this != null) for (i in 0 until length()) list.add(optString(i)); return list }
    private fun JSONObject?.toStringMap(): MutableMap<String, String> { val map = mutableMapOf<String, String>(); if (this != null) { val keys = keys(); while (keys.hasNext()) { val key = keys.next(); map[key] = optString(key) } }; return map }
    private fun JSONArray?.toWorkers(): MutableList<Worker> {
        val list = mutableListOf<Worker>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(Worker(o.optString("name"), o.optString("role"), o.optString("signatureB64"), o.optString("signedAt")))
        }
        return list
    }
    private fun JSONArray?.toHistory(): MutableList<PtHistoryItem> {
        val list = mutableListOf<PtHistoryItem>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(
                PtHistoryItem(
                    ptNumber = o.optString("ptNumber"),
                    emittedAt = o.optString("emittedAt"),
                    place = o.optString("place"),
                    company = o.optString("company"),
                    responsible = o.optString("responsible"),
                    fileName = o.optString("fileName"),
                    status = o.optString("status", "LIBERADA"),
                    startMillis = o.optLong("startMillis", 0L),
                    endMillis = o.optLong("endMillis", 0L),
                    closedAt = o.optString("closedAt"),
                    closeNote = o.optString("closeNote")
                )
            )
        }
        return list
    }
}
