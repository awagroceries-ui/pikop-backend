const db = require('../config/db');

/**
 * Gets or creates an open support conversation for the authenticated participant.
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
      return res.status(200).json(rows[0]);
    }

    // 2. Create new one
    const createRes = await db.query(
      "INSERT INTO conversations (participant_id, participant_type, status) VALUES ($1, $2, 'OPEN') RETURNING id, status",
      [userId, participantType]
    );

    res.status(201).json(createRes.rows[0]);
  } catch (error) {
    console.error('Support Conversation Error:', error);
    res.status(500).json({ error: 'Failed to initialize support chat' });
  }
};

/**
 * Fetches message history for a specific conversation.
 */
const getMessages = async (req, res) => {
  const { conversationId } = req.params;
  const userId = req.user.id;

  try {
    // Authorization Check: Must be the participant or an admin
    const isAdmin = !!req.session?.adminId;
    if (!isAdmin) {
      const convRes = await db.query("SELECT id FROM conversations WHERE id = $1 AND participant_id = $2", [conversationId, userId]);
      if (convRes.rows.length === 0) return res.status(403).json({ error: 'Unauthorized' });
    }

    const { rows } = await db.query(
      "SELECT * FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC LIMIT 100",
      [conversationId]
    );

    res.status(200).json(rows);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch messages' });
  }
};

module.exports = {
  getOrCreateConversation,
  getMessages
};
