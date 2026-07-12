import type { Metadata } from "next";
import { Geist, Geist_Mono, Inter, Noto_Sans_JP } from "next/font/google";
import "./globals.css";
import SiteNav from "./SiteNav";
import { ToastProvider } from "./Toast";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
});

const notoSansJP = Noto_Sans_JP({
  variable: "--font-noto-jp",
  subsets: ["latin"],
  weight: ["400", "500", "700"],
});

export const metadata: Metadata = {
  title: {
    template: "%s | テックプラザ",
    default: "テックプラザ",
  },
  description: "毎日安い！家電もガジェットもまとめてお得に買えるECサイト",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} ${inter.variable} ${notoSansJP.variable}`}
    >
      <body>
        <ToastProvider>
          <SiteNav />
          {children}
        </ToastProvider>
      </body>
    </html>
  );
}
