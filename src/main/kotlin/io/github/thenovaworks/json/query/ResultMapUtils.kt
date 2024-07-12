package io.github.thenovaworks.json.query

class ResultMapUtils {
    companion object {

        fun toMap(obj: Any?): Map<String, Any> {
            return obj as Map<String, Any>
        }

        fun toList(obj: Any?): List<Map<String, Any>> {
            return obj as List<Map<String, Any>>
        }
    }
}