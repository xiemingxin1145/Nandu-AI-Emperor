package com.xiemingxin.nandu

import com.xiemingxin.nandu.ui.EmperorViewModel

/**
 * Temporary named-argument compatibility shim.
 * MainActivity historically used both `cityId =` and `cid =` when calling recruitInCity.
 * Kotlin named arguments are part of source-call resolution, so keep this tiny alias until
 * MainActivity is next consolidated. It delegates to the canonical cityId API and changes no game logic.
 */
fun EmperorViewModel.recruitInCity(cid: String, unitId: String) {
    recruitInCity(cityId = cid, unitId = unitId)
}
