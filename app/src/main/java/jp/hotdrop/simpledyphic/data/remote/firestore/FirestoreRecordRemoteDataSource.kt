package jp.hotdrop.simpledyphic.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.mapper.toConditionType
import jp.hotdrop.simpledyphic.data.mapper.toRawCondition
import jp.hotdrop.simpledyphic.domain.model.Record
import kotlinx.coroutines.tasks.await

class FirestoreRecordRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : RecordRemoteDataSource {
    override suspend fun findAll(userId: String): List<Record> {
        val snapshot = firestore.collection(ROOT_COLLECTION)
            .document(userId)
            .collection(RECORD_ROOT_COLLECTION)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { map ->
                runCatching {
                    Record(
                        id = doc.id.toInt(),
                        breakfast = getString(map, RECORD_BREAKFAST_FIELD),
                        lunch = getString(map, RECORD_LUNCH_FIELD),
                        dinner = getString(map, RECORD_DINNER_FIELD),
                        isToilet = getBoolean(map, RECORD_IS_TOILET_FIELD),
                        condition = getString(map, RECORD_CONDITION_FIELD).toConditionType(),
                        conditionMemo = getString(map, RECORD_CONDITION_MEMO_FIELD),
                        stepCount = getInt(map, RECORD_STEP_COUNT_FIELD),
                        healthKcal = getDouble(map, RECORD_HEALTH_KCAL_FIELD),
                        ringfitKcal = getDouble(map, RECORD_RINGFIT_KCAL_FIELD),
                        ringfitKm = getDouble(map, RECORD_RINGFIT_KM_FIELD)
                    )
                }.getOrNull()
            }
        }
    }

    override suspend fun saveAll(userId: String, records: List<Record>) {
        for (record in records) {
            val map = HashMap<String, Any>()
            record.breakfast?.takeIf { it.isNotBlank() }?.let { map[RECORD_BREAKFAST_FIELD] = it }
            record.lunch?.takeIf { it.isNotBlank() }?.let { map[RECORD_LUNCH_FIELD] = it }
            record.dinner?.takeIf { it.isNotBlank() }?.let { map[RECORD_DINNER_FIELD] = it }
            map[RECORD_IS_TOILET_FIELD] = record.isToilet
            record.condition?.toRawCondition()?.let { map[RECORD_CONDITION_FIELD] = it }
            record.conditionMemo?.takeIf { it.isNotBlank() }?.let { map[RECORD_CONDITION_MEMO_FIELD] = it }
            record.stepCount?.let { map[RECORD_STEP_COUNT_FIELD] = it }
            record.healthKcal?.let { map[RECORD_HEALTH_KCAL_FIELD] = it }
            record.ringfitKcal?.let { map[RECORD_RINGFIT_KCAL_FIELD] = it }
            record.ringfitKm?.let { map[RECORD_RINGFIT_KM_FIELD] = it }

            firestore.collection(ROOT_COLLECTION)
                .document(userId)
                .collection(RECORD_ROOT_COLLECTION)
                .document(record.id.toString())
                .set(map, SetOptions.merge())
                .await()
        }
    }

    private fun getString(map: Map<String, Any?>, fieldName: String): String? {
        return (map[fieldName] as? String)?.ifBlank { null }
    }

    private fun getBoolean(map: Map<String, Any?>, fieldName: String): Boolean {
        return map[fieldName] as? Boolean ?: false
    }

    private fun getInt(map: Map<String, Any?>, fieldName: String): Int? {
        return (map[fieldName] as? Number)?.toInt()
    }

    private fun getDouble(map: Map<String, Any?>, fieldName: String): Double? {
        return (map[fieldName] as? Number)?.toDouble()
    }

    private companion object {
        private const val ROOT_COLLECTION = "dyphic"
        private const val RECORD_ROOT_COLLECTION = "records"
        private const val RECORD_BREAKFAST_FIELD = "breakfast"
        private const val RECORD_LUNCH_FIELD = "lunch"
        private const val RECORD_DINNER_FIELD = "dinner"
        private const val RECORD_IS_TOILET_FIELD = "isToilet"
        private const val RECORD_CONDITION_FIELD = "condition"
        private const val RECORD_CONDITION_MEMO_FIELD = "conditionMemo"
        private const val RECORD_STEP_COUNT_FIELD = "stepCount"
        private const val RECORD_HEALTH_KCAL_FIELD = "healthKcal"
        private const val RECORD_RINGFIT_KCAL_FIELD = "ringfitKcal"
        private const val RECORD_RINGFIT_KM_FIELD = "ringfitKm"
    }
}
