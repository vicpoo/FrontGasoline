package org.vicpoo.gasolineproject.core.session

import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LicenseRulesTest {

    @Test
    fun driverWithoutLicense_mustUploadIt() {
        val session = testSession(privileges = setOf(Privileges.VIAJE_REGISTRAR), requiresLicense = true)
        assertTrue(session.drives)
        assertTrue(session.mustUploadLicense)
    }

    @Test
    fun userWhoDoesNotDrive_isNeverAskedForLicense() {
        val session = testSession(privileges = setOf(Privileges.SOLICITUD_APROBAR), requiresLicense = true)
        assertFalse(session.drives)
        assertFalse(session.mustUploadLicense)
    }

    @Test
    fun userWithoutEmployee_doesNotDrive() {
        val session = testSession(privileges = setOf(Privileges.VIAJE_REGISTRAR), employeeId = null, requiresLicense = true)
        assertFalse(session.mustUploadLicense)
    }

    @Test
    fun driverWithLicense_isNotAskedAgain() {
        val session = testSession(privileges = setOf(Privileges.VIAJE_REGISTRAR), requiresLicense = false)
        assertFalse(session.mustUploadLicense)
    }
}
