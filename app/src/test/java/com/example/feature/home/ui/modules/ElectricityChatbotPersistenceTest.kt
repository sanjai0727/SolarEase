package com.example.feature.home.ui.modules

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.User
import com.example.core.session.UserSession
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ElectricityChatbotPersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        UserSession.init(context)
        UserSession.currentUser = User(
            id = "test_user_123",
            username = "sanjai_test",
            name = "Sanjai Kumar",
            ebServiceNumber = "040123456789",
            mobileNumber = "9876543210"
        )
        ChatHistoryStorage.resetForTesting()
        ChatHistoryStorage.init(context)
    }

    @After
    fun tearDown() {
        ChatHistoryStorage.resetForTesting()
        UserSession.currentUser = null
    }

    @Test
    fun testInitialEmptyState_whenNoHistoryExists() {
        val viewModel = ElectricityChatbotViewModel()
        val state = viewModel.uiState.value

        assertTrue("Messages should be empty initially", state.messages.isEmpty())
        assertNull("Active session ID should be null initially", state.activeSessionId)
        assertTrue("Sessions list should be empty", state.sessions.isEmpty())
        assertFalse("History drawer should be closed initially", state.showHistory)
    }

    @Test
    fun testSessionPersistence_acrossReopen() {
        val userId = "test_user_123"
        val session1 = ChatSession(
            id = "session_abc",
            userId = userId,
            title = "What is the solar subsidy?",
            snippet = "PM Surya Ghar provides up to Rs 78,000 subsidy.",
            timestamp = System.currentTimeMillis() - 5000,
            messages = listOf(
                ChatMessage(isUser = true, text = "What is the solar subsidy?"),
                ChatMessage(isUser = false, text = "PM Surya Ghar provides up to Rs 78,000 subsidy.")
            )
        )

        // Save session in storage and mark it as active
        ChatHistoryStorage.saveSession(session1)
        ChatHistoryStorage.setActiveSessionId(userId, session1.id)

        // Simulate leaving and re-opening the chatbot (new ViewModel created)
        val reopenedViewModel = ElectricityChatbotViewModel()
        val state = reopenedViewModel.uiState.value

        assertEquals("Should restore the active session ID", "session_abc", state.activeSessionId)
        assertEquals("Should restore all messages in the conversation", 2, state.messages.size)
        assertEquals("What is the solar subsidy?", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("PM Surya Ghar provides up to Rs 78,000 subsidy.", state.messages[1].text)
        assertFalse(state.messages[1].isUser)
    }

    @Test
    fun testStartNewChat_resetsToEmptyState_andPreservesHistory() {
        val userId = "test_user_123"
        val session1 = ChatSession(
            id = "session_prev",
            userId = userId,
            title = "How to reduce my EB bill?",
            snippet = "Install a 3kW solar system.",
            timestamp = System.currentTimeMillis() - 10000,
            messages = listOf(
                ChatMessage(isUser = true, text = "How to reduce my EB bill?"),
                ChatMessage(isUser = false, text = "Install a 3kW solar system.")
            )
        )
        ChatHistoryStorage.saveSession(session1)
        ChatHistoryStorage.setActiveSessionId(userId, session1.id)

        // Open with previous chat
        val viewModel = ElectricityChatbotViewModel()
        assertEquals(2, viewModel.uiState.value.messages.size)

        // User taps "+" to start a new chat
        viewModel.startNewChat()

        val newState = viewModel.uiState.value
        assertTrue("Messages should now be empty (empty state)", newState.messages.isEmpty())
        assertNull("Active session should be null in new chat", newState.activeSessionId)
        assertEquals("Previous conversation must remain in history", 1, newState.sessions.size)
        assertEquals("session_prev", newState.sessions[0].id)
    }

    @Test
    fun testLoadSessionFromHistory() {
        val userId = "test_user_123"
        val session1 = ChatSession(
            id = "thread_1",
            userId = userId,
            title = "Thread 1",
            snippet = "Answer 1",
            timestamp = 1000L,
            messages = listOf(ChatMessage(isUser = true, text = "Thread 1"))
        )
        val session2 = ChatSession(
            id = "thread_2",
            userId = userId,
            title = "Thread 2",
            snippet = "Answer 2",
            timestamp = 2000L,
            messages = listOf(ChatMessage(isUser = true, text = "Thread 2"))
        )
        ChatHistoryStorage.saveSession(session1)
        ChatHistoryStorage.saveSession(session2)

        val viewModel = ElectricityChatbotViewModel()
        // Select thread 1 from history
        viewModel.loadSession(session1)

        val state = viewModel.uiState.value
        assertEquals("thread_1", state.activeSessionId)
        assertEquals(1, state.messages.size)
        assertEquals("Thread 1", state.messages[0].text)
        assertFalse("History sheet should close after selecting session", state.showHistory)
    }

    @Test
    fun testDeleteSession() {
        val userId = "test_user_123"
        val session = ChatSession(
            id = "to_delete",
            userId = userId,
            title = "To be deleted",
            snippet = "Deleted snippet",
            messages = listOf(ChatMessage(isUser = true, text = "Delete me"))
        )
        ChatHistoryStorage.saveSession(session)
        ChatHistoryStorage.setActiveSessionId(userId, session.id)

        val viewModel = ElectricityChatbotViewModel()
        viewModel.deleteSession("to_delete")

        val state = viewModel.uiState.value
        assertTrue("Messages should be reset", state.messages.isEmpty())
        assertTrue("Sessions should be empty", state.sessions.isEmpty())
        assertNull("Active session should be null", state.activeSessionId)
    }
}
