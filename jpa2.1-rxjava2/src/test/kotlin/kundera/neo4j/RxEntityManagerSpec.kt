package kundera.neo4j

import Neo4jBook
import net.eraga.jpa.async.*
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import kotlin.test.*

/**
 * Date: 08/05/2018
 * Time: 21:23
 */
object RxEntityManagerSpec : SubjectSpek<String>({
    System.setProperty("org.jboss.logging.provider", "slf4j")

    subject { "neo4j Kundera" }

    lateinit var entityManager: EntityManager
    lateinit var entityManagerFactory: EntityManagerFactory

    given("$subject persistence unit") {
        beforeEachTest {

            RxPersistence
                    .createEntityManagerFactory(subject)
                    .flatMap {
                        entityManagerFactory = it
                        it.rxCreateEntityManager()
                    }.map {
                        entityManager = it
                    }
                    .blockingGet()


        }

        afterEachTest {
            entityManager.close()
            entityManagerFactory.closeAndDeleteDBFiles(subject)
        }

        on("persisting entity in db") {
            val book = Neo4jBook("rxPersistBook", 1)
            it("should persist Book in DB") {
                val error = entityManager
                        .rxPersist(book)
                        .blockingGet()

                error?.printStackTrace()
                assertNull(error, error?.message)
            }
//          //TODO file a bug to Kundera
//            it("should update changed field in db") {
//                book.text = "persisted text"
//                entityManager.rxFlush().blockingGet()
//                entityManager.clear()
//
//                val bookNew = entityManager
//                        .rxFind(Neo4jBook::class.java, 1)
//                        .blockingGet()
//
//                assertNotNull(bookNew)
//
//                assertEquals("persisted text", bookNew.text)
//            }
        }

        on("merging entity to DB") {
            val book = Neo4jBook("rxMergeBook", 1)
            lateinit var bookMerged: Neo4jBook

            it("should merge Book in DB") {
                bookMerged = entityManager
                        .rxMerge(book)
                        .blockingGet()


                val bookModified = entityManager
                        .rxFind(Neo4jBook::class.java, 1)
                        .blockingGet()

                assertEquals("rxMergeBook", bookModified.title)
            }

            it("should not update field of original entity in db") {
                book.text = "persisted text"


                val bookNew = entityManager
                        .rxFind(Neo4jBook::class.java, 1)
                        .blockingGet()

                assertNotNull(bookNew)

                assertNotEquals("persisted text", bookNew.text)
            }

            it("should update field of merged entity in db") {
                bookMerged.text = "persisted text"

                val bookNew = entityManager
                        .rxFind(Neo4jBook::class.java, 1)
                        .blockingGet()


                assertEquals("persisted text", bookNew.text)
            }
        }
//        //TODO file a bug to Kundera, this should throw an exception
//        on("removing non existent entity from DB") {
//            it("should fail to remove") {
//                assertFails {
//                    entityManager
//                            .rxFind(Neo4jBook::class.java, 12000)
//                            .blockingGet()
//                }
//
//                assertFailsWith<IllegalArgumentException> {
//                    val error = entityManager
//                            .rxRemove(Neo4jBook().apply { id = 12000 })
//                            .blockingGet()
//
//                    log.info("error: {}",  error)
//                    if (error != null)
//                        throw error
//                }
//            }
//        }

        on("removing detached entity from DB") {
            it("should succeed to remove") {
                val book = Neo4jBook("To be not deleted", 1)
                entityManager
                        .rxPersist(book)
                        .blockingGet()


                val error = entityManager
                        .rxRemove(Neo4jBook().apply { id = 1 })
                        .blockingGet()

                error?.printStackTrace()
                assertNull(error, error?.message)

                assertFails {
                    entityManager
                            .rxFind(Neo4jBook::class.java, 1)
                            .blockingGet()
                }
            }
        }

        on("removing attached entity from DB") {
            it("should remove entity") {
                val book = Neo4jBook("To be deleted", 1)
                entityManager
                        .rxPersist(book)
                        .blockingGet()

                val error = entityManager
                        .rxRemove(book)
                        .blockingGet()

                error?.printStackTrace()
                assertNull(error, error?.message)

                assertFails {
                    entityManager
                            .rxFind(Neo4jBook::class.java, 1)
                            .blockingGet()
                }
            }
        }



        on("rxTransaction") {
            it("should store 4 books in transaction") {
                entityManager
                        .rxTransaction {
                            it.persist(Neo4jBook("Transaction book 1", 1))
                            it.persist(Neo4jBook("Transaction book 2", 2))
                            it.persist(Neo4jBook("Transaction book 3", 3))
                            it.persist(Neo4jBook("Transaction book 4", 4))
                        }
                        .blockingGet()

                assertNotNull(entityManager.rxFind(Neo4jBook::class.java, 4))
            }
        }
    }
})
