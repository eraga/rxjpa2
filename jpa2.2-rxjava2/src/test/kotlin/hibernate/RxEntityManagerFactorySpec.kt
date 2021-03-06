package hibernate

import net.eraga.jpa.async.*
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.SynchronizationType
import kotlin.test.assertEquals
import kotlin.test.assertFails

/**
 * Date: 08/05/2018
 * Time: 22:39
 */
object RxEntityManagerFactorySpec : SubjectSpek<String>({
    System.setProperty("org.jboss.logging.provider", "slf4j")

    subject { "H2 Hibernate" }

    var entityManager: EntityManager? = null
    lateinit var entityManagerFactory: EntityManagerFactory


    given("$subject persistence unit") {
        beforeEachTest {
            entityManagerFactory = RxPersistence
                    .createEntityManagerFactory(subject)
                    .blockingGet()
        }

        afterEachTest {
            entityManager?.close()
            entityManager = null

            entityManagerFactory.close()
        }

        on("create manager with properties ") {
            it("should use properties") {
                val properties = HashMap<String, Any>()
                properties["org.hibernate.flushMode"] = "COMMIT"
                entityManager = entityManagerFactory
                        .rxCreateEntityManager(properties)
                        .blockingGet()
                assertEquals("COMMIT", entityManager?.properties?.get("org.hibernate.flushMode"))
            }
        }

        on("create manager with synchronizationType ") {
            it("should fail to create SYNCHRONIZED because we have no JTA") {
                assertFails {
                    entityManager = entityManagerFactory
                            .rxCreateEntityManager(SynchronizationType.SYNCHRONIZED)
                            .blockingGet()
                }
            }

            it("should fail to create UNSYNCHRONIZED because we have no JTA") {
                assertFails {
                    entityManager = entityManagerFactory
                            .rxCreateEntityManager(SynchronizationType.SYNCHRONIZED)
                            .blockingGet()
                }
            }
        }

        on("create manager with properties and synchronizationType ") {
            it("should also fail because we have no JTA") {
                val properties = HashMap<String, Any>()
                properties["javax.persistence.jdbc.driver"] = "unknown_db"
                assertFails {
                    entityManager = entityManagerFactory
                            .rxCreateEntityManager(SynchronizationType.SYNCHRONIZED, properties)
                            .blockingGet()
                }

            }
        }
    }
})
