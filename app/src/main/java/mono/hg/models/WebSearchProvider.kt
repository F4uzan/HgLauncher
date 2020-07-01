package mono.hg.models

class WebSearchProvider {
    var url: String?
        private set
    var name: String?
        private set
    var id: String?
        private set

    constructor(name: String?, url: String?) {
        this.url = url
        this.name = name
        id = name
    }

    constructor(name: String?, url: String?, id: String?) {
        this.url = url
        this.name = name
        this.id = id
    }

    override fun equals(obj: Any?): Boolean {
        val `object` = obj as WebSearchProvider?

        // URL can be shared, but names should stay unique.
        return if (`object` != null) {
            name == `object`.name || this.javaClass == obj!!.javaClass
        } else {
            false
        }
    }
}