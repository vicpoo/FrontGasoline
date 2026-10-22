package org.vicpoo.gasolineproject.core.rbac

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessRuleTest {

    @Test
    fun anyOf_requiresAtLeastOnePrivilege() {
        val rule = AccessRule.anyOf("a", "b")
        assertTrue(rule.isSatisfiedBy(setOf("b")))
        assertFalse(rule.isSatisfiedBy(setOf("c")))
        assertFalse(rule.isSatisfiedBy(emptySet()))
    }

    @Test
    fun allOf_requiresEveryPrivilege() {
        val rule = AccessRule.allOf("a", "b")
        assertTrue(rule.isSatisfiedBy(setOf("a", "b", "c")))
        assertFalse(rule.isSatisfiedBy(setOf("a")))
    }

    @Test
    fun everyone_isAlwaysSatisfied() {
        assertTrue(AccessRule.Everyone.isSatisfiedBy(emptySet()))
    }

    @Test
    fun key_doesNotDependOnOrder() {
        assertEquals(AccessRule.anyOf("b", "a").key, AccessRule.anyOf("a", "b").key)
    }

    @Test
    fun homeTab_needsToSeeCarsAndCreateRequests() {
        // Quien solo administra carros no ve Inicio (ya los ve en Flota)
        assertFalse(AppAccess.HOME_TAB.isSatisfiedBy(setOf(Privileges.CARRO_VER, Privileges.CARRO_CREAR)))
        assertTrue(AppAccess.HOME_TAB.isSatisfiedBy(setOf(Privileges.CARRO_VER, Privileges.SOLICITUD_CREAR)))
    }

    @Test
    fun fleetTab_opensWithAnyManagementPrivilege() {
        assertTrue(AppAccess.FLEET_TAB.isSatisfiedBy(setOf(Privileges.CARRO_ELIMINAR)))
        assertFalse(AppAccess.FLEET_TAB.isSatisfiedBy(setOf(Privileges.CARRO_VER)))
    }

    @Test
    fun tripHistory_opensWithOwnOrAllTrips() {
        assertTrue(AppAccess.TRIP_HISTORY.isSatisfiedBy(setOf(Privileges.VIAJE_VER_PROPIOS)))
        assertTrue(AppAccess.TRIP_HISTORY.isSatisfiedBy(setOf(Privileges.VIAJE_VER_TODOS)))
        assertFalse(AppAccess.TRIP_HISTORY.isSatisfiedBy(setOf(Privileges.VIAJE_REGISTRAR)))
    }

    @Test
    fun fuelForm_opensToCreateOrToEdit() {
        assertTrue(AppAccess.FUEL_FORM.isSatisfiedBy(setOf(Privileges.CARGA_EDITAR)))
        assertFalse(AppAccess.FUEL_FORM.isSatisfiedBy(setOf(Privileges.CARGA_ELIMINAR)))
    }
}
