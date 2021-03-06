import javax.persistence.*


/**
 * Date: 08/05/2018
 * Time: 18:24
 */
@Entity
data class Neo4jBook(
        var title: String = "",
        @Id
        @Column(name = "id")
        var id: Int = (Math.random() * 1000).toInt(),
        var text: String = "",
        var num: Int = (Math.random() * 100).toInt()
) {
}
