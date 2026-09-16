const db = require('../src/config/db');

async function audit() {
  try {
    const vendors = await db.query("SELECT id, business_name, status FROM vendors WHERE status = 'pending'");
    const kitchens = await db.query("SELECT id, business_name, status FROM kitchens WHERE status = 'pending'");

    console.log(`Unverified Vendors (Old Flow): ${vendors.rows.length}`);
    vendors.rows.forEach(v => console.log(` - ${v.business_name} (ID: ${v.id})`));

    console.log(`Unverified Kitchens (Old Flow): ${kitchens.rows.length}`);
    kitchens.rows.forEach(k => console.log(` - ${k.business_name} (ID: ${k.id})`));

    process.exit(0);
  } catch (e) {
    console.error(e);
    process.exit(1);
  }
}

audit();
