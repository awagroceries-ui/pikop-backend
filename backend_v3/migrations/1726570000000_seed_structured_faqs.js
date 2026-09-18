exports.up = (pgm) => {
  // 1. Update target_audience constraint
  pgm.dropConstraint('knowledge_base', 'knowledge_base_target_audience_check');
  pgm.addConstraint('knowledge_base', 'knowledge_base_target_audience_check', {
    check: "target_audience IN ('CUSTOMER', 'FULFILLER', 'MERCHANT', 'CORPORATE', 'BOTH')"
  });

  // 2. Clear existing entries to prevent duplication/mess
  pgm.sql("DELETE FROM knowledge_base");

  // 3. Seed structured data
  const faqs = [
    // CUSTOMER - Getting Started
    { group: 'CUSTOMER', cat: 'Getting Started', q: `What can I do on Pikop?`, a: `Pikop offers four things from one app: Dispatch (send or receive a package point-to-point), Food (order from restaurants and kitchens), Groceries (order food products and household items), and Shop (order general goods from local sellers). Each has its own dedicated section on your home screen.` },
    { group: 'CUSTOMER', cat: 'Getting Started', q: `Do I need to create a separate account for each service?`, a: `No. One Customer account covers Dispatch, Food, Groceries, and Shop.` },
    { group: 'CUSTOMER', cat: 'Getting Started', q: `Is Pikop available in my city?`, a: `Pikop currently operates in Port Harcourt, Lagos, and Abuja, with more cities planned. If Pikop isn't live in your area yet, you can still create an account and join the waitlist to be notified when we launch there.` },

    // CUSTOMER - COD & Payments
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `What is Cash on Delivery (COD) on Pikop, and is it actually cash?`, a: `Despite the name, no cash changes hands. COD on Pikop means you pay upfront through the app, but your money is held securely by Pikop and only released to the seller once you confirm your item has arrived and is correct. This protects you from paying for something you never receive or that isn't as described.` },
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `When does the seller actually get paid?`, a: `Once you confirm you've received your item and it matches what you ordered — or, if you don't respond, automatically after a set waiting period if you haven't raised any concern.` },
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `What if my item doesn't arrive, or arrives damaged or wrong?`, a: `Don't confirm receipt. Instead, use the "Report a Problem" option to flag the issue before the waiting period ends. Our team will review it and can refund you if your claim is upheld.` },
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `Once I confirm an order is correct, can I still get a refund?`, a: `Confirming receipt and correctness closes Pikop's involvement in that order — this is what allows sellers to be paid promptly. If you have a genuine issue after confirming, that becomes a matter between you and the seller directly, so it's worth taking a moment to actually check your item before confirming.` },
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `What payment methods can I use?`, a: `Card, bank transfer, USSD, or other options shown at checkout, depending on what's enabled. COD (escrow) is also available where the seller accepts it.` },
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `Is there a fee for using COD?`, a: `Yes, a small platform fee applies to COD orders, shown clearly in your order summary before you pay — it's never hidden inside the item price.` },
    { group: 'CUSTOMER', cat: 'COD & Payments', q: `Can I pay from my Pikop Wallet?`, a: `Yes, if your wallet balance covers the order total, you can choose to pay from your wallet at checkout instead of a fresh card/transfer payment.` },

    // CUSTOMER - Dispatch
    { group: 'CUSTOMER', cat: 'Dispatch', q: `How does Dispatch work?`, a: `Enter a pickup location and a drop-off location, and a nearby available fulfiller (foot agent, cyclist, rider, or driver) will be matched to your request. You'll see live tracking once a fulfiller is on the way.` },
    { group: 'CUSTOMER', cat: 'Dispatch', q: `Can I send something to someone who isn't a Pikop user?`, a: `Yes. If the receiver doesn't have the app, Pikop sends them what they need by SMS — a payment link if payment is needed from them, a live tracking link so they can watch the delivery progress, and a confirmation link once it arrives.` },
    { group: 'CUSTOMER', cat: 'Dispatch', q: `What are the pickup and delivery codes for?`, a: `These are short codes you share with the fulfiller at pickup and with the receiver at delivery, confirming the right person is handing over or receiving the item.` },
    { group: 'CUSTOMER', cat: 'Dispatch', q: `Can someone else pay for a delivery going to me, or vice versa?`, a: `Yes — the person paying, the person sending, and the person receiving can all be different people. Whoever the item is actually addressed to is the one asked to confirm it arrived correctly.` },
    { group: 'CUSTOMER', cat: 'Dispatch', q: `Can I schedule a delivery for later instead of right now?`, a: `Yes, you can choose "Schedule for later" at checkout and pick a future date and time instead of dispatching immediately.` },

    // CUSTOMER - Food, Groceries, Shop, Ratings
    { group: 'CUSTOMER', cat: 'Food', q: `How is Food different from Groceries?`, a: `Food covers prepared meals from restaurants and home kitchens. Groceries covers raw/packaged food products and household items you'd typically shop for, not ready-to-eat meals.` },
    { group: 'CUSTOMER', cat: 'Food', q: `Can I order Food using COD?`, a: `Only from Food merchants who've chosen to accept COD orders — this is shown on the merchant's page before you order.` },
    { group: 'CUSTOMER', cat: 'Groceries', q: `Are grocery prices on Pikop different from in-store prices?`, a: `Pikop keeps its commission on groceries low specifically so prices stay close to what you'd pay in-store — you're mainly paying for the convenience of delivery.` },
    { group: 'CUSTOMER', cat: 'Shop', q: `What kind of items can I find in Shop?`, a: `General goods from local sellers — anything outside the Food and Groceries categories.` },
    { group: 'CUSTOMER', cat: 'Ratings', q: `Why should I rate my fulfiller?`, a: `Ratings feed into each fulfiller's verification tier (Basic, Standard, Elite, Super), which helps maintain quality across the platform and rewards consistently good fulfillers.` },
    { group: 'CUSTOMER', cat: 'Ratings', q: `I'm getting an error trying to submit a rating. What should I do?`, a: `Try again after a moment, and if it persists, contact support with the order details — this has been a known issue we're actively fixing.` },

    // FULFILLER - Getting Started
    { group: 'FULFILLER', cat: 'Getting Started', q: `What are the different Fulfiller categories?`, a: `Foot Agent/Cyclist (on foot or bicycle, identity verification only), Rider (motorcycle, requires vehicle documents and a license, plus a commercial permit in some cities), and Driver (car/van, requires vehicle documents, license, and — where applicable — insurance and roadworthiness documents).` },
    { group: 'FULFILLER', cat: 'Getting Started', q: `Which category should I choose?`, a: `Whichever matches how you actually plan to fulfill missions — your category determines both your verification requirements and what's shown on your public profile.` },
    { group: 'FULFILLER', cat: 'Getting Started', q: `How long does verification take?`, a: `This depends on document review times and can vary — you'll be notified as soon as a decision is made.` },
    { group: 'FULFILLER', cat: 'Getting Started', q: `I completed verification but my app still shows "Verify Account." What's wrong?`, a: `Try closing and reopening the app to refresh your status. If it persists after your account has actually been approved, contact support — this is a known sync issue we're actively fixing.` },

    // FULFILLER - Missions
    { group: 'FULFILLER', cat: 'Missions', q: `What details do I see before accepting a mission?`, a: `Pickup and drop-off information, estimated distance/fare, and importantly, whether it's a delivery-only mission or a delivery-plus-COD mission — so you know what's involved before you commit.` },
    { group: 'FULFILLER', cat: 'Missions', q: `If I'm a Rider or Foot Agent/Cyclist, can I accept missions at any time?`, a: `For safety reasons, Foot Agent/Cyclist and Rider dispatch is limited to 6:00am–6:00pm. Outside those hours, only Drivers are matched to new missions. If you're already on a mission when 6:00pm passes, you're not interrupted — you can complete it normally.` },
    { group: 'FULFILLER', cat: 'Missions', q: `I accidentally pressed Back after accepting a mission. Did I lose it?`, a: `No — an accepted mission stays assigned to you regardless of navigating around the app. You can always find your active mission from your home screen.` },
    { group: 'FULFILLER', cat: 'Missions', q: `For a COD mission, am I collecting cash from the buyer?`, a: `No, never. Every COD order is fully paid upfront into escrow before pickup. You are never expected to collect cash from anyone.` },
    { group: 'FULFILLER', cat: 'Missions', q: `What's the SOS button for?`, a: `If something goes wrong during a mission, tap and hold the SOS button to immediately alert Pikop with your live location. If you've registered a trusted contact, they can be notified too.` },

    // FULFILLER - Earnings & Payout
    { group: 'FULFILLER', cat: 'Earnings & Payout', q: `How much do I earn per delivery?`, a: `You receive 75% of the delivery fee for each completed mission; Pikop retains 25%. This split applies to every delivery, regardless of whether the order also involves COD.` },
    { group: 'FULFILLER', cat: 'Earnings & Payout', q: `Does a buyer's dispute over an item affect my earnings?`, a: `No. Your earnings are paid out once you've completed the delivery, entirely separate from whatever happens with the item itself afterward. A dispute between the buyer and seller has no effect on what you've already earned.` },
    { group: 'FULFILLER', cat: 'Earnings & Payout', q: `My completed missions aren't showing in my history. What's wrong?`, a: `This is a known issue we're actively fixing — contact support with your account details if this is still happening after a recent app update.` },
    { group: 'FULFILLER', cat: 'Earnings & Payout', q: `My wallet balance shows ₦0 despite completed missions. What's wrong?`, a: `Also a known issue being actively fixed — contact support if this persists.` },
    { group: 'FULFILLER', cat: 'Earnings & Payout', q: `I can't enter my bank details for payout. What's wrong?`, a: `This was a known bug that's been addressed — try again, and if the fields are still locked, contact support.` },
    { group: 'FULFILLER', cat: 'Earnings & Payout', q: `How does Pikop verify my bank account before payout?`, a: `You select your bank from a list and enter your account number; Pikop verifies the account and shows you the registered account name to confirm before saving, so payout errors from mistyped numbers are caught early.` },

    // MERCHANT - Getting Started
    { group: 'MERCHANT', cat: 'Getting Started', q: `What are the Merchant categories?`, a: `Food, Groceries, and Shop — matching the three customer-facing modules. Each carries its own commission rate.` },
    { group: 'MERCHANT', cat: 'Getting Started', q: `What does onboarding involve?`, a: `Two stages: verifying you as the individual contact person for the business (identity verification), and verifying the business itself (business details, CAC registration, address, bank details for payout).` },
    { group: 'MERCHANT', cat: 'Getting Started', q: `Do I need a NAFDAC number to sell on Pikop?`, a: `Only if your category involves regulated consumables — packaged food, cosmetics, or similar. It's not required for general goods.` },
    { group: 'MERCHANT', cat: 'Getting Started', q: `I can't seem to start adding products. What's wrong?`, a: `This can happen if your account hasn't fully completed business verification yet, or if you're experiencing a known display bug — check your verification status first, and contact support if you're fully approved but still blocked.` },

    // MERCHANT - Fees & Commission
    { group: 'MERCHANT', cat: 'Fees & Commission', q: `What commission does Pikop take?`, a: `Food: 10%, Groceries: 5%, Shop: 10% — deducted from your payout on each sale, not added to what the customer pays.` },
    { group: 'MERCHANT', cat: 'Fees & Commission', q: `Does the commission rate ever change without notice?`, a: `The rate applied to any given order is whatever was in effect at the time that order was placed — a later rate change never affects orders you've already completed.` },
    { group: 'MERCHANT', cat: 'Fees & Commission', q: `Do I pay commission even on non-COD orders?`, a: `Yes — the marketplace commission applies to every sale regardless of payment method. It's the COD platform fee (separate, paid by the buyer) that only applies to COD orders specifically.` },

    // MERCHANT - COD & Payments
    { group: 'MERCHANT', cat: 'COD & Payments', q: `Am I required to accept COD orders?`, a: `No — you can choose during onboarding, and change your mind later in settings, whether to accept COD orders at all. If you don't, customers only see prepaid checkout for your products.` },
    { group: 'MERCHANT', cat: 'COD & Payments', q: `If I accept COD, does the 10% COD fee cost me anything?`, a: `No — the COD platform fee is paid entirely by the buyer, on top of the item price. It's never deducted from your payout.` },
    { group: 'MERCHANT', cat: 'COD & Payments', q: `When do I actually get paid for an order?`, a: `Once the buyer confirms they've received the item and it's correct, or after the waiting period passes if they don't respond and no dispute is raised.` },

    // MERCHANT - Operating Hours & Returns
    { group: 'MERCHANT', cat: 'Operating Hours & Orders', q: `Can I set my own opening hours?`, a: `Yes — configure your operating hours in your business settings. Outside those hours, your storefront shows as closed and customers can't place new orders with you.` },
    { group: 'MERCHANT', cat: 'Operating Hours & Orders', q: `How do I know if an order is COD?`, a: `Order details clearly show the payment type and status before you need to act on it.` },
    { group: 'MERCHANT', cat: 'Returns', q: `Am I required to accept returns?`, a: `No — you set your own return policy (whether you accept returns, the return window, any excluded categories) in your business settings, and it's shown to customers before they buy.` }
  ];

  for (const f of faqs) {
    pgm.sql(`INSERT INTO knowledge_base (title, content, category, target_audience, priority) VALUES ($1, $2, $3, $4, 0)`, [f.q, f.a, f.cat, f.group]);
  }
};

exports.down = (pgm) => {
  pgm.sql("DELETE FROM knowledge_base");
};
