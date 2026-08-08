const db = require('../src/config/db');
const bcrypt = require('bcrypt');

/**
 * CLI script to create an initial admin user.
 * Usage: node scratch/create_admin.js <username> <password>
 */
const createAdmin = async () => {
  const args = process.argv.slice(2);
  if (args.length < 2) {
    console.log('Usage: node scratch/create_admin.js <username> <password>');
    process.exit(1);
  }

  const [username, password] = args;
  const role = 'super_admin';

  try {
    console.log(`Creating admin account: ${username}...`);
    const passwordHash = await bcrypt.hash(password, 10);

    const query = 'INSERT INTO admin_users (username, password_hash, role) VALUES ($1, $2, $3) RETURNING id';
    const { rows } = await db.query(query, [username, passwordHash, role]);

    console.log('✅ Admin account created successfully! ID:', rows[0].id);
    process.exit(0);
  } catch (error) {
    if (error.code === '23505') {
      console.error('❌ Error: Username already exists.');
    } else {
      console.error('❌ Failed to create admin:', error.message);
    }
    process.exit(1);
  }
};

createAdmin();
