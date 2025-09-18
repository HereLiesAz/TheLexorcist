package com.hereliesaz.lexorcist.model

object StringConstants {
    object String {
        const val Action1 = "evidence.text.includes(\"\")"
        const val Action2 = "evidence.tags.includes(\"\")"
        const val Action3 = "evidence.documentDate > new Date(\"YYYY-MM-DD\").getTime()"
        const val Action4 = "evidence.documentDate < new Date(\"YYYY-MM-DD\").getTime()"
        const val Action5 = "if (evidence.content.includes(\"threaten\")) {\n    parser.tags.push(\"Threat\");\n}"
        const val Action6 = "if (evidence.content.includes(\"invoice\") || evidence.content.includes(\"receipt\")) {\n    parser.tags.push(\"Financial\");\n}"
    }
}