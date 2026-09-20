const db = require('../config/db');

/**
 * Fetches knowledge base articles filtered by the user's role.
 */
const getKnowledgeBase = async (req, res) => {
  const { group } = req.query;
  // Fallback to user's actual role if no group specified, default to CUSTOMER
  const userRole = group || req.user.role || 'CUSTOMER';

  try {
    const { rows } = await db.query(
      `SELECT id, title, content, category, priority
       FROM knowledge_base
       WHERE is_active = true
       AND (target_audience = $1 OR target_audience = 'BOTH')
       ORDER BY category ASC, priority DESC`,
      [userRole]
    );

    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

/**
 * Fetches a single knowledge base article by ID.
 */
const getArticleById = async (req, res) => {
    const { articleId } = req.params;
    try {
        const { rows } = await db.query(
            "SELECT id, title, content, category, priority FROM knowledge_base WHERE id = $1",
            [articleId]
        );
        if (rows.length === 0) return res.status(404).json({ success: false, message: 'Article not found' });
        res.status(200).json(rows[0]);
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
};

/**
 * Gets or creates an open support conversation.
 */
const getOrCreateConversation = async (req, res) => {
  const userId = req.user.id;
  const participantType = req.user.role === 'FULFILLER' ? 'FULFILLER' : 'USER';

  try {
    // 1. Check for existing open conversation
    const { rows } = await db.query(
      "SELECT id, status FROM conversations WHERE participant_id = $1 AND participant_type = $2 AND status = 'OPEN' LIMIT 1",
      [userId, participantType]
    );

    if (rows.length > 0) {
      // FLATTEN: Return fields directly for Android App compatibility
      return res.status(200).json({ id: rows[0].id, status: rows[0].status });
    }

    // 2. Create new one
    const createRes = await db.query(
      "INSERT INTO conversations (participant_id, participant_type, status) VALUES ($1, $2, 'OPEN') RETURNING id, status",
      [userId, participantType]
    );

    res.status(201).json({ id: createRes.rows[0].id, status: createRes.rows[0].status });
  } catch (error) {
    throw error;
  }
};

const getSupportInbox = async (req, res) => {
    try {
        const { rows } = await db.query(`
            SELECT c.*, u.full_name as participant_name,
            (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND is_read = false AND sender_type != 'ADMIN') as unread_count
            FROM conversations c
            JOIN users u ON u.id = c.participant_id
            WHERE c.status = 'OPEN'
            ORDER BY c.last_message_at DESC
        `);
        res.render('support_inbox', { conversations: rows });
    } catch (error) {
        res.status(500).send(error.message);
    }
};

/**
 * Fetches message history for a specific conversation.
 */
const getMessages = async (req, res) => {
  const { conversationId } = req.params;

  if (!conversationId || conversationId === 'null' || conversationId === 'undefined') {
      console.warn('[Support] getMessages called with invalid ID:', conversationId);
      return res.status(200).json([]);
  }

  try {
    // Mark as read when messages are fetched by admin or user
    await db.query("UPDATE messages SET is_read = true WHERE conversation_id = $1 AND sender_type != 'ADMIN'", [conversationId]);

    const { rows } = await db.query(
      "SELECT id, sender_id, sender_type, content, content as text, content as body, created_at, is_read FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC LIMIT 100",
      [conversationId]
    );

    res.status(200).json(rows);
  } catch (error) {
    throw error;
  }
};

/**
 * Interactive AI Assistant using Gemini (v4.7).
 */
const askPikopAgent = async (req, res) => {
    const { question } = req.body;
    const userRole = req.user.role;

    try {
        // 1. Fetch relevant context from Knowledge Base (RAG-lite)
        const { rows: articles } = await db.query(
            `SELECT title, content FROM knowledge_base
             WHERE is_active = true
             AND (target_audience = $1 OR target_audience = 'BOTH')
             ORDER BY priority DESC LIMIT 5`,
            [userRole]
        );

        const context = articles.map(a => `Q: ${a.title}\nA: ${a.content}`).join('\n\n');

        const prompt = `
            Context: You are the Pikop Support Agent, a helpful AI assistant for the Pikop app (a Nigerian logistics and marketplace platform).
            User Question: "${question}"
            User Role: ${userRole}

            Knowledge Base Context:
            ${context}

            Rules:
            1. Use ONLY the information provided in the context if applicable.
            2. If the context doesn't answer the question, be helpful but suggest contacting a human agent for complex issues.
            3. Keep the tone friendly, professional, and concise. Use "₦" for currency.
            4. Do not make up fake policies.

            Response:
        `;

        const { GoogleGenerativeAI } = require("@google/generative-ai");
        const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
        const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash-latest" });

        const result = await model.generateContent(prompt);
        const responseText = result.response.text();

        res.status(200).json({ success: true, answer: responseText });

    } catch (error) {
        console.error('[GeminiAgent] Error:', error.message);
        res.status(500).json({ success: false, message: 'AI Assistant is temporarily busy.' });
    }
};

module.exports = {
  getKnowledgeBase,
  getArticleById,
  getOrCreateConversation,
  getMessages,
  getSupportInbox,
  askPikopAgent
};
