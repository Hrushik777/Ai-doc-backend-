/**
 * Deciding which extracted value belongs in which template column.
 *
 * <p>Three tiers, in order of precedence: structural (a table's header band),
 * deterministic (an exact header-name match), and semantic (the model, given only
 * what the first two could not resolve).
 */
package com.example.ai_doc.pipeline.mapping;
