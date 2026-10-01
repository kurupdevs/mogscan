package com.kurupdevs.moggr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kurupdevs.moggr.analysis.FeatureScore
import com.kurupdevs.moggr.analysis.Improvement
import com.kurupdevs.moggr.analysis.LandmarkPt
import com.kurupdevs.moggr.analysis.PillarScore
import com.kurupdevs.moggr.analysis.PslReport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists the last analysis so the user never has to re-scan.
 * Report goes to internal storage as JSON, front photo as JPEG.
 */
object ReportStore {
    private const val REPORT_FILE = "last_report.json"
    private const val PHOTO_FILE = "last_face.jpg"

    fun save(context: Context, report: PslReport, frontPhoto: Bitmap?) {
        try {
            File(context.filesDir, REPORT_FILE).writeText(reportToJson(report).toString())
            frontPhoto?.let {
                File(context.filesDir, PHOTO_FILE).outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun loadReport(context: Context): PslReport? {
        return try {
            val f = File(context.filesDir, REPORT_FILE)
            if (!f.exists()) null else reportFromJson(JSONObject(f.readText()))
        } catch (_: Exception) {
            null
        }
    }

    fun loadPhoto(context: Context): Bitmap? {
        return try {
            val f = File(context.filesDir, PHOTO_FILE)
            if (!f.exists()) null else BitmapFactory.decodeFile(f.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun hasSaved(context: Context): Boolean =
        File(context.filesDir, REPORT_FILE).exists()

    private fun reportToJson(r: PslReport): JSONObject {
        val o = JSONObject()
        o.put("overallPsl", r.overallPsl)
        o.put("overall100", r.overall100)
        o.put("summary", r.summary)
        o.put("anglesRead", r.anglesRead)
        o.put("decile", r.decile)
        o.put("percentile", r.percentile)
        o.put("failoCount", r.failoCount)
        o.put("haloCount", r.haloCount)
        val feats = JSONArray()
        r.features.forEach { f ->
            feats.put(JSONObject().put("name", f.name).put("score", f.score).put("note", f.note))
        }
        o.put("features", feats)
        val strengths = JSONArray()
        r.strengths.forEach { strengths.put(it) }
        o.put("strengths", strengths)
        val imps = JSONArray()
        r.improvements.forEach { i ->
            imps.put(JSONObject().put("area", i.area).put("method", i.method).put("effort", i.effort))
        }
        o.put("improvements", imps)
        val pillars = JSONArray()
        r.pillars.forEach { p ->
            pillars.put(JSONObject().put("name", p.name).put("score", p.score).put("note", p.note))
        }
        o.put("pillars", pillars)
        val notes = JSONArray()
        r.photoNotes.forEach { notes.put(it) }
        o.put("photoNotes", notes)
        o.put("confidence", r.confidence)
        o.put("uncertainty", r.uncertainty)
        o.put("potentialPsl", r.potentialPsl)
        val mesh = JSONArray()
        r.landmarkMesh.forEach { p ->
            mesh.put(JSONArray().put(p.x.toDouble()).put(p.y.toDouble()).put(p.kind))
        }
        o.put("landmarkMesh", mesh)
        val box = JSONArray()
        r.faceBox.forEach { box.put(it.toDouble()) }
        o.put("faceBox", box)
        val thirds = JSONArray()
        r.thirdsY.forEach { thirds.put(it.toDouble()) }
        o.put("thirdsY", thirds)
        o.put("faceShape", r.faceShape)
        o.put("faceShapeNote", r.faceShapeNote)
        o.put("skinUndertone", r.skinUndertone)
        o.put("canthalTiltDeg", r.canthalTiltDeg)
        o.put("timestamp", r.timestamp)
        val details = JSONObject()
        r.measureDetails.forEach { (k, v) -> details.put(k, v) }
        o.put("measureDetails", details)
        return o
    }

    private fun reportFromJson(o: JSONObject): PslReport {
        val features = mutableListOf<FeatureScore>()
        val fa = o.optJSONArray("features") ?: JSONArray()
        for (i in 0 until fa.length()) {
            val f = fa.getJSONObject(i)
            features.add(FeatureScore(f.optString("name"), f.optDouble("score"), f.optString("note")))
        }
        val strengths = mutableListOf<String>()
        val sa = o.optJSONArray("strengths") ?: JSONArray()
        for (i in 0 until sa.length()) strengths.add(sa.optString(i))
        val improvements = mutableListOf<Improvement>()
        val ia = o.optJSONArray("improvements") ?: JSONArray()
        for (i in 0 until ia.length()) {
            val im = ia.getJSONObject(i)
            improvements.add(Improvement(im.optString("area"), im.optString("method"), im.optString("effort")))
        }
        val pillars = mutableListOf<PillarScore>()
        val pa = o.optJSONArray("pillars") ?: JSONArray()
        for (i in 0 until pa.length()) {
            val p = pa.getJSONObject(i)
            pillars.add(PillarScore(p.optString("name"), p.optDouble("score"), p.optString("note")))
        }
        val notes = mutableListOf<String>()
        val na = o.optJSONArray("photoNotes") ?: JSONArray()
        for (i in 0 until na.length()) notes.add(na.optString(i))
        val mesh = mutableListOf<LandmarkPt>()
        val ma = o.optJSONArray("landmarkMesh") ?: JSONArray()
        for (i in 0 until ma.length()) {
            val p = ma.optJSONArray(i) ?: continue
            if (p.length() >= 3) mesh.add(LandmarkPt(p.optDouble(0).toFloat(), p.optDouble(1).toFloat(), p.optInt(2)))
        }
        val box = mutableListOf<Float>()
        val ba = o.optJSONArray("faceBox") ?: JSONArray()
        for (i in 0 until ba.length()) box.add(ba.optDouble(i).toFloat())
        val thirds = mutableListOf<Float>()
        val ta = o.optJSONArray("thirdsY") ?: JSONArray()
        for (i in 0 until ta.length()) thirds.add(ta.optDouble(i).toFloat())
        val details = mutableMapOf<String, String>()
        val da = o.optJSONObject("measureDetails")
        if (da != null) {
            val keys = da.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                details[k] = da.optString(k)
            }
        }
        return PslReport(
            overallPsl = o.optDouble("overallPsl"),
            overall100 = o.optInt("overall100"),
            features = features,
            strengths = strengths,
            improvements = improvements,
            summary = o.optString("summary"),
            anglesRead = o.optInt("anglesRead", 3),
            decile = o.optDouble("decile"),
            percentile = o.optInt("percentile"),
            pillars = pillars,
            photoNotes = notes,
            failoCount = o.optInt("failoCount"),
            haloCount = o.optInt("haloCount"),
            landmarkMesh = mesh,
            faceBox = box,
            thirdsY = thirds,
            confidence = o.optDouble("confidence"),
            uncertainty = o.optDouble("uncertainty", 0.5),
            potentialPsl = o.optDouble("potentialPsl"),
            faceShape = o.optString("faceShape"),
            faceShapeNote = o.optString("faceShapeNote"),
            measureDetails = details,
            skinUndertone = o.optString("skinUndertone"),
            canthalTiltDeg = o.optDouble("canthalTiltDeg"),
            timestamp = o.optLong("timestamp")
        )
    }
}
