// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BertTokenizer.encode].
 *
 * Tests use [BertTokenizer.forTesting] with a small injected vocabulary so no
 * Android context or assets are required, making them runnable as pure JUnit tests.
 *
 * Structural invariants under test:
 *  - Output arrays are always exactly MAX_LEN (128) long
 *  - [CLS] token is always at position 0 with attention mask = 1
 *  - [SEP] token is placed immediately after the last real token
 *  - Positions after [SEP] are PAD (id=0, mask=0)
 *  - Token IDs are looked up from vocab; unknown tokens map to UNK_ID (100)
 *  - encode() on an uninitialised tokenizer returns a zero-attention encoding
 */
class BertTokenizerTest {

    private val smallVocab = mapOf(
        "[CLS]"  to BertTokenizer.CLS_ID,
        "[SEP]"  to BertTokenizer.SEP_ID,
        "[UNK]"  to BertTokenizer.UNK_ID,
        "[PAD]"  to BertTokenizer.PAD_ID,
        "hello"  to 200,
        "world"  to 201,
        "spam"   to 202,
        "otp"    to 203,
        "##ing"  to 204
    )

    private lateinit var tokenizer: BertTokenizer

    @Before
    fun setup() {
        tokenizer = BertTokenizer.forTesting(smallVocab)
    }

    // ── Output shape ─────────────────────────────────────────────────────────────

    @Test
    fun `output arrays are exactly MAX_LEN long`() {
        val enc = tokenizer.encode("hello world")
        assertEquals(BertTokenizer.MAX_LEN, enc.inputIds.size)
        assertEquals(BertTokenizer.MAX_LEN, enc.attentionMask.size)
        assertEquals(BertTokenizer.MAX_LEN, enc.tokenTypeIds.size)
    }

    @Test
    fun `all tokenTypeIds are zero for single-segment input`() {
        val enc = tokenizer.encode("hello world")
        assertTrue(enc.tokenTypeIds.all { it == 0L })
    }

    // ── CLS token ────────────────────────────────────────────────────────────────

    @Test
    fun `CLS token is at position 0`() {
        val enc = tokenizer.encode("hello")
        assertEquals(BertTokenizer.CLS_ID.toLong(), enc.inputIds[0])
    }

    @Test
    fun `attention mask is 1 at CLS position`() {
        val enc = tokenizer.encode("hello")
        assertEquals(1L, enc.attentionMask[0])
    }

    // ── SEP token ────────────────────────────────────────────────────────────────

    @Test
    fun `SEP token is placed immediately after last real token`() {
        // "hello world" → 2 tokens → SEP at position 3 (0=CLS, 1=hello, 2=world, 3=SEP)
        val enc = tokenizer.encode("hello world")
        assertEquals(BertTokenizer.SEP_ID.toLong(), enc.inputIds[3])
        assertEquals(1L, enc.attentionMask[3])
    }

    @Test
    fun `single token input has SEP at position 2`() {
        val enc = tokenizer.encode("spam")
        assertEquals(BertTokenizer.CLS_ID.toLong(),  enc.inputIds[0])
        assertEquals(202L,                             enc.inputIds[1])  // spam
        assertEquals(BertTokenizer.SEP_ID.toLong(),  enc.inputIds[2])
    }

    // ── Padding ──────────────────────────────────────────────────────────────────

    @Test
    fun `positions after SEP are padded with id=0 and mask=0`() {
        val enc = tokenizer.encode("hello")
        // CLS=0, hello=1, SEP=2, then 3..127 should be PAD
        for (i in 3 until BertTokenizer.MAX_LEN) {
            assertEquals("ids[$i] should be PAD",  BertTokenizer.PAD_ID.toLong(), enc.inputIds[i])
            assertEquals("mask[$i] should be 0",   0L, enc.attentionMask[i])
        }
    }

    @Test
    fun `empty input produces CLS SEP then all padding`() {
        val enc = tokenizer.encode("")
        assertEquals(BertTokenizer.CLS_ID.toLong(), enc.inputIds[0])
        assertEquals(BertTokenizer.SEP_ID.toLong(), enc.inputIds[1])
        for (i in 2 until BertTokenizer.MAX_LEN) {
            assertEquals(BertTokenizer.PAD_ID.toLong(), enc.inputIds[i])
            assertEquals(0L, enc.attentionMask[i])
        }
    }

    // ── Vocabulary lookup ────────────────────────────────────────────────────────

    @Test
    fun `known token maps to correct vocab id`() {
        val enc = tokenizer.encode("spam")
        assertEquals(202L, enc.inputIds[1])
    }

    @Test
    fun `unknown token maps to UNK_ID`() {
        val enc = tokenizer.encode("xyzzy")
        assertEquals(BertTokenizer.UNK_ID.toLong(), enc.inputIds[1])
    }

    // ── Uninitialised state ──────────────────────────────────────────────────────

    @Test
    fun `uninitialised tokenizer returns zero-attention encoding`() {
        val uninit = BertTokenizer()  // default constructor, never initialised
        assertFalse(uninit.isReady)
        val enc = uninit.encode("hello world")
        assertEquals(BertTokenizer.MAX_LEN, enc.inputIds.size)
        assertTrue("All masks should be 0", enc.attentionMask.all { it == 0L })
        assertTrue("All ids should be PAD", enc.inputIds.all { it == BertTokenizer.PAD_ID.toLong() })
    }

    // ── Truncation ───────────────────────────────────────────────────────────────

    @Test
    fun `long input is truncated to MAX_LEN`() {
        // Build a string with many tokens that would exceed MAX_LEN
        val longText = List(200) { "hello" }.joinToString(" ")
        val enc = tokenizer.encode(longText)
        assertEquals(BertTokenizer.MAX_LEN, enc.inputIds.size)
        // SEP must be present at the last non-padding position
        val lastReal = enc.attentionMask.indexOfLast { it == 1L }
        assertEquals(BertTokenizer.SEP_ID.toLong(), enc.inputIds[lastReal])
    }
}
