import type { Metadata, Viewport } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "AnishOS — Your watch, reimagined",
  description: "The companion dashboard for AnishOS on Wear OS.",
  applicationName: "AnishOS",
  icons: { icon: "/icon.svg" }
};
export const viewport: Viewport = { themeColor: "#050914", colorScheme: "dark" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
