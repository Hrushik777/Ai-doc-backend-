package com.example.ai_doc.auth;

/**
 * Who Google says the user is, taken from a token whose signature has already been verified.
 *
 * <p>{@code subject} is Google's stable identifier for the account and is what a user record
 * would be keyed on if one is ever stored. The email is what a person recognises, but it is the
 * weaker identifier: Google accounts can change their primary address, and the same address can
 * in principle be reissued.
 */
public record GoogleIdentity(String subject, String email, String name) {
}
