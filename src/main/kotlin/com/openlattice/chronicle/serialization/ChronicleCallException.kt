package com.openlattice.chronicle.serialization

/**
 *
 * @author Matthew Tamayo-Rios &lt;matthew@getmethodic.com&gt;
 */
public class ChronicleCallException : RuntimeException {
    public val body: String
    public val code: Int
    public val url: String

    public constructor(message: String, url: String, body: String, code: Int) : super(message) {
        this.code = code
        this.body = body
        this.url = url
    }

    public constructor(message: String, cause: Throwable, url:String, body: String, code: Int) : super(message, cause) {
        this.code = code
        this.body = body
        this.url = url
    }
}