package nl.vlugverkoop.model

/**
 * A listing that can check its own data before it is stored.
 *
 * Each vehicle type that implements this has its own rules, but all of them report
 * problems in the same form: one message per problem. The routes send that list
 * back to the app in a 400 response.
 */
interface Validatable {

    /**
     * Checks this listing.
     *
     * @return one message per problem; an empty list means the data is valid.
     */
    fun validate(): List<String>
}
