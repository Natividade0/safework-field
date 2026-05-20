package com.safefield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class AprRepository(context: Context) {
    private val prefs = context.getSharedPreferences("safefield_apr", Context.MODE_PRIVATE)

    fun load(): AprData {
        val json = prefs.getString("draft_json", null) ?: return AprData(aprNumber = AprEngine.aprNumber())
        return runCatching {
            val o = JSONObject(json)
            AprData(
                aprNumber = o.optString("aprNumber").ifBlank { AprEngine.aprNumber(o.optLong("dateMillis", System.currentTimeMillis())) },
                company = o.optString("company"),
                area = o.optString("area"),
                place = o.optString("place"),
                responsible = o.optString("responsible"),
                activityDescription = o.optString("activityDescription"),
                tools = o.optString("tools"),
                observations = o.optString("observations"),
                dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                selectedActivities = o.optJSONArray("selectedActivities").toStringSet(),
                manualActivity = o.optString("manualActivity"),
                items = o.optJSONArray("items").toAprItems(),
                workers = o.optJSONArray("workers").toAprWorkers(),
                photos = o.optJSONArray("photos").toStringList(),
                responsibleSignatureB64 = o.optString("responsibleSignatureB64"),
                history = o.optJSONArray("history").toAprHistory()
            )
        }.getOrElse { AprData(aprNumber = AprEngine.aprNumber()) }
    }

    fun save(data: AprData): Unit {
        prefs.edit().putString("draft_json", toJson(data).toString()).apply()
    }

    fun clearDraftKeepHistory(data: AprData): AprData {
        val next = AprData(aprNumber = AprEngine.aprNumber())
        next.history = data.history
        save(next)
        return next
    }

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

    private fun toJson(data: AprData): JSONObject = JSONObject().apply {
        put("aprNumber", data.aprNumber)
        put("company", data.company)
        put("area", data.area)
        put("place", data.place)
        put("responsible", data.responsible)
        put("activityDescription", data.activityDescription)
        put("tools", data.tools)
        put("observations", data.observations)
        put("dateMillis", data.dateMillis)
        put("selectedActivities", JSONArray(data.selectedActivities.toList()))
        put("manualActivity", data.manualActivity)
        put("photos", JSONArray(data.photos))
        put("responsibleSignatureB64", data.responsibleSignatureB64)
        put("items", JSONArray().also { arr -> data.items.forEach { arr.put(it.toJson()) } })
        put("workers", JSONArray().also { arr ->
            data.workers.forEach { arr.put(JSONObject().put("name", it.name).put("role", it.role).put("signatureB64", it.signatureB64).put("signedAt", it.signedAt)) }
        })
        put("history", JSONArray().also { arr ->
            data.history.forEach {
                arr.put(
                    JSONObject()
                        .put("aprNumber", it.aprNumber)
                        .put("emittedAt", it.emittedAt)
                        .put("company", it.company)
                        .put("place", it.place)
                        .put("responsible", it.responsible)
                        .put("fileName", it.fileName)
                        .put("status", it.status)
                )
            }
        })
    }

    private fun AprItem.toJson(): JSONObject = JSONObject()
        .put("activity", activity)
        .put("step", step)
        .put("danger", danger)
        .put("risk", risk)
        .put("consequence", consequence)
        .put("control", control)
        .put("epi", epi)
        .put("severity", severity)
        .put("probability", probability)
        .put("classification", classification)

    private fun JSONArray?.toStringSet(): MutableSet<String> {
        val set = linkedSetOf<String>()
        if (this != null) for (i in 0 until length()) set.add(optString(i))
        return set
    }

    private fun JSONArray?.toStringList(): MutableList<String> {
        val list = mutableListOf<String>()
        if (this != null) for (i in 0 until length()) list.add(optString(i))
        return list
    }

    private fun JSONArray?.toAprItems(): MutableList<AprItem> {
        val list = mutableListOf<AprItem>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(
                AprItem(
                    activity = o.optString("activity"),
                    danger = o.optString("danger"),
                    risk = o.optString("risk"),
                    consequence = o.optString("consequence"),
                    control = o.optString("control"),
                    epi = o.optString("epi", o.optString("recommendedEpi")),
                    severity = o.optInt("severity", 3),
                    probability = o.optInt("probability", 2),
                    classification = o.optString("classification", o.optString("riskLevel", "MEDIO")),
                    step = o.optString("step")
                )
            )
        }
        return list
    }

    private fun JSONArray?.toAprWorkers(): MutableList<AprWorker> {
        val list = mutableListOf<AprWorker>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(AprWorker(o.optString("name"), o.optString("role"), o.optString("signatureB64"), o.optString("signedAt")))
        }
        return list
    }

    private fun JSONArray?.toAprHistory(): MutableList<AprHistoryItem> {
        val list = mutableListOf<AprHistoryItem>()
        if (this != null) for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            list.add(
                AprHistoryItem(
                    aprNumber = o.optString("aprNumber"),
                    emittedAt = o.optString("emittedAt"),
                    company = o.optString("company"),
                    place = o.optString("place"),
                    responsible = o.optString("responsible"),
                    fileName = o.optString("fileName"),
                    status = o.optString("status", "EMITIDA")
                )
            )
        }
        return list
    }
}
