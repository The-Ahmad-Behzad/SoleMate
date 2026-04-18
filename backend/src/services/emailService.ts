import nodemailer from 'nodemailer';

const ADMIN_EMAIL = process.env.ADMIN_EMAIL || 'admin@solemate.app';

function createTransporter() {
  // Use SMTP settings from env; falls back to Ethereal for dev
  if (process.env.EMAIL_HOST) {
    return nodemailer.createTransport({
      host: process.env.EMAIL_HOST,
      port: Number(process.env.EMAIL_PORT || 587),
      secure: process.env.EMAIL_SECURE === 'true',
      auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS,
      },
    });
  }
  // Development: log emails to console, don't actually send
  return null;
}

export async function sendAdminIntegrationNotification(params: {
  sellerEmail: string;
  sellerCompany: string;
  shoeName: string;
  brand: string;
  s3Url: string;
  requestNote?: string;
  integrationRequestId: string;
  catalogueEntry: object;
}): Promise<void> {
  const {
    sellerEmail, sellerCompany, shoeName, brand,
    s3Url, requestNote, integrationRequestId, catalogueEntry,
  } = params;

  const subject = `[SoleMate] New AR Lens Integration Request — ${shoeName} by ${sellerCompany}`;
  const html = `
    <div style="font-family: Arial, sans-serif; max-width: 600px;">
      <h2 style="color: #6c47ff;">New Integration Request</h2>
      <table style="width:100%; border-collapse:collapse;">
        <tr><td style="padding:8px; font-weight:bold; width:140px;">Seller</td><td>${sellerEmail} (${sellerCompany})</td></tr>
        <tr><td style="padding:8px; font-weight:bold;">Shoe Name</td><td>${shoeName}</td></tr>
        <tr><td style="padding:8px; font-weight:bold;">Brand</td><td>${brand}</td></tr>
        <tr><td style="padding:8px; font-weight:bold;">Request ID</td><td><code>${integrationRequestId}</code></td></tr>
        <tr><td style="padding:8px; font-weight:bold;">GLB URL</td><td><a href="${s3Url}">${s3Url}</a></td></tr>
        ${requestNote ? `<tr><td style="padding:8px; font-weight:bold;">Note</td><td>${requestNote}</td></tr>` : ''}
      </table>
      <h3>Catalogue Entry (copy to catalogue.json)</h3>
      <pre style="background:#f5f5f5; padding:12px; border-radius:6px; overflow-x:auto;">${JSON.stringify(catalogueEntry, null, 2)}</pre>
      <p style="color:#888; font-size:12px;">This is an automated notification from the SoleMate Seller Portal.</p>
    </div>
  `;

  const transporter = createTransporter();
  if (!transporter) {
    console.log('[emailService] DEV MODE — would have sent admin email:');
    console.log('  To:', ADMIN_EMAIL);
    console.log('  Subject:', subject);
    console.log('  Integration Request ID:', integrationRequestId);
    return;
  }

  await transporter.sendMail({
    from: `"SoleMate Portal" <${process.env.EMAIL_USER || 'noreply@solemate.app'}>`,
    to: ADMIN_EMAIL,
    subject,
    html,
  });

  console.log(`[emailService] Admin notification sent for integration request ${integrationRequestId}`);
}
