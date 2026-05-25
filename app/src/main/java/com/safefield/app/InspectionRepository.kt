package com.safefield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class InspectionRepository(context: Context) {
    private val prefs = context.getSharedPreferences("safefield_inspection", Context.MODE_PRIVATE)

    fun load(): InspectionData {
        val json = prefs.getString("draft_json", null) ?: return InspectionData(number = InspectionEngine.number())
        return runCatching {
            val o = JSONObject(json)
            InspectionData(
                number = o.optString("number").ifBlank { InspectionEngine.number(o.optLong("dateMillis", System.currentTimeMillis())) },
                company = o.optString("company"),
                area = o.optString("area"),
                place = o.optString("place"),
                inspector = o.optString("inspector"),
                objective = o.optString("objective"),
                dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                records = o.optJSONArray("records").toRecords(),
                inspectorSignatureB64 = o.optString("inspectorSignatureB64"),
                history = o.optJSONArray("history").toHistory()
            )
        }.getOrElse { InspectionData(number = InspectionEngine.number()) }
    }

    fun save(data: InspectionData): Unit {
        prefs.edit().putString("draft_json", toJson(data).toString()).apply()
    }

    fun clearDraftKeepHistory(data: InspectionData): InspectionData {
        val next = InspectionData(number = InspectionEngine.number())
        next.history = data.history
        save(next)
        return next
    }

    fun base64ToBitmap(value: String): Bitmap? {
        if (value.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun toJson(data: InspectionData): JSONObject = JSONObject().apply {
        put("number", data.number)
        put("company", data.company)
        put("area", data.area)
        put("place", data.place)
        put("inspector", data.inspector)
        put("objective", data.objective)
        put("dateMillis", data.dateMillis)
        put("inspectorSignatureB64", data.inspectorSignatureB64)
        put("records", JSONArray().also { arr -> data.records.forEach { arr.put(it.toJson()) } })
        put("history", JSONArray().also { arr ->
            data.history.forEach {
                arr.put(JSONObject()
                    .put("number", it.number)
                    .put("generatedAt", it.generatedAt)
                    .put("company", it.company)
                    .put("place", it.place)
                    .put("inspector", it.inspector)
                    .put("fileName", it.fileName))
            }
        })
    }

    private fun InspectionRecord.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("category", category)
        .put("location", location)
        .put("description", description)
        .put("risk", risk)
        .put("recommendation", recommendation)
        .put("responsible", responsible)
        .put("deadline", deadline)
        .put("priority", priority)
        .put("status", status)
        .put("notes", notes)
        .put("photos", JSONArray(photos))

    private fun JSONArray?.toRecords(): MutableList<InspectionRecord> {
        val list = mutableListOf<InspectionRecord>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(InspectionRecord(
                id = o.optLong("id", System.currentTimeMillis()),
                category = o.optString("category", "Condição insegura"),
                location = o.optString("location"),
                description = o.optString("description"),
                risk = o.optString("risk"),
                recommendation = o.optString("recommendation"),
                responsible = o.optString("responsible"),
                deadline = o.optString("deadline"),
                priority = o.optString("priority", "Média"),
                status = o.optString("status", "Aberto"),
                notes = o.optString("notes"),
                photos = o.optJSONArray("photos").toStringList()
            ))
        }
        return list
    }

    private fun JSONArray?.toHistory(): MutableList<InspectionHistoryItem> {
        val list = mutableListOf<InspectionHistoryItem>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(InspectionHistoryItem(
                number = o.optString("number"),
                generatedAt = o.optString("generatedAt"),
                company = o.optString("company"),
                place = o.optString("place"),
                inspector = o.optString("inspector"),
                fileName = o.optString("fileName")
            ))
        }
        return list
    }

    private fun JSONArray?.toStringList(): MutableList<String> {
        val list = mutableListOf<String>()
        if (this != null) for (i in 0 until length()) list.add(optString(i))
        return list
    }
}
