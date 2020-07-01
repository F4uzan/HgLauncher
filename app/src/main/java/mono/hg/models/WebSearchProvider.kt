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

    override fun equals(other: Any?): Boolean {
        val `object` = other as WebSearchProvider?

        // URL can be shared, but names should stay unique.
        return if (`object` != null) {
            name == `object`.name || this.javaClass == other!!.javaClass
        } else {
            false
        }
    }
    override fun hashCode(): Int {
        var result = url?.hashCode() ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }
}