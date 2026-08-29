/**
 * The document-to-spreadsheet pipeline, and the service that orchestrates it.
 *
 * <p>Sub-packages read in stage order:
 * {@code validation} -> {@code understanding} -> {@code layout} -> {@code mapping}
 * -> {@code excel}. Each stage depends on {@code domain} types rather than on the
 * stage before it, so any one of them can be exercised on its own.
 *
 * <p>The ordering principle throughout is deterministic-first: every stage resolves
 * what it can from the document's own structure, and only what is genuinely left
 * over reaches a model.
 */
package com.example.ai_doc.pipeline;
