const db = require('../config/db');

/**
 * Fetches knowledge base articles filtered by the user's role.
 */
const getKnowledgeBase = async (req, res) => {
  const userRole = req.user.role === 'FULFILLER' ? 'FULFILLER' : 'CUSTOMER';

  try {
    const { rows } = await db.query(
      `SELECT id, title, content, category, priority
       FROM knowledge_base
       WHERE is_active = true
       AND (target_audience = $1 OR target_audience = 'BOTH')
       ORDER BY category ASC, priority DESC`,
      [userRole]
    );

    // FLATTEN: Return raw array for the Android app
    res.status(200).json(rows);
  } catch (error) {
    throw error;
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

module.exports = {
  getKnowledgeBase,
  getOrCreateConversation,
  getMessages,
  getSupportInbox
};
