package com.xiemingxin.nandu.agent

/**
 * Stage 8 compatibility aliases.
 *
 * The first Character Agent implementation used the names IntentProposal and
 * AgentConflict in GameState while the final implementation exposes
 * AgentProposal and plain conflict text. Keep these aliases at the boundary so
 * older call sites and V7 save/state code stay source-compatible without
 * duplicating models.
 */
typealias IntentProposal = AgentProposal
typealias AgentConflict = String
