package com.openlattice.chronicle.settings

/**
 * @author alfoncenzioka &lt;alfonce@openlattice.com&gt;
 */
public enum class AppComponent(private val component: String) {
    CHRONICLE("chronicle"),
    CHRONICLE_DATA_COLLECTION("chronicle_data_collection"),
    CHRONICLE_SURVEYS("chronicle_surveys"),
    TIME_USE_DIARY("time_use_diary");

    override fun toString(): String {
        return component
    }

    public companion object {
        public fun fromString(name: String): AppComponent {
            for (component in values()) {
                if (component.toString() == name) {
                    return component
                }
            }
            throw IllegalArgumentException("No app component with name $name")
        }
    }
}