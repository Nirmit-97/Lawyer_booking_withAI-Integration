import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';

function LandingPage() {
    useEffect(() => {
        // Add dark mode to html element when on LandingPage
        document.documentElement.classList.add('dark');

        // Cleanup: remove dark mode when leaving LandingPage
        return () => {
            document.documentElement.classList.remove('dark');
        };
    }, []);

    return (
        <div className="landing-page bg-background-light dark:bg-justice-dark text-slate-900 dark:text-slate-100 font-display antialiased overflow-x-hidden">
            <style>
                {`
        .glass {
            background: rgba(255, 255, 255, 0.03);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.1);
        }
        .glass-primary {
            background: rgba(13, 89, 242, 0.05);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(13, 89, 242, 0.2);
        }
        .hero-glow {
            background: radial-gradient(circle at 50% 50%, rgba(13, 89, 242, 0.15) 0%, rgba(10, 25, 47, 0) 70%);
        }
        .text-glow {
            text-shadow: 0 0 20px rgba(13, 89, 242, 0.5);
        }
        `}
            </style>

            {/* Sticky Glassmorphic Navbar */}
            <nav className="fixed top-0 w-full z-50 px-6 py-4">
                <div className="max-w-7xl mx-auto flex items-center justify-between glass rounded-xl px-6 py-3">
                    <div className="flex items-center gap-2">
                        <div className="w-10 h-10 bg-justice-blue rounded flex items-center justify-center">
                            <span className="material-icons text-white">gavel</span>
                        </div>
                        <span className="text-xl font-extrabold tracking-tight text-slate-900 dark:text-white">
                            Legal<span className="text-justice-blue">Connect</span>
                        </span>
                    </div>
                    <div className="hidden md:flex items-center gap-8 text-sm font-medium">
                        <a className="hover:text-justice-blue transition-colors text-slate-900 dark:text-white" href="#solutions">Solutions</a>
                        <a className="hover:text-justice-blue transition-colors text-slate-900 dark:text-white" href="#tech">Technology</a>
                        <a className="hover:text-justice-blue transition-colors text-slate-900 dark:text-white" href="#security">Security</a>
                        <a className="hover:text-justice-blue transition-colors text-slate-900 dark:text-white" href="#contact">Contact</a>
                    </div>
                    <div className="flex items-center gap-4">
                        <Link
                            to="/lawyer-login"
                            className="px-5 py-2 text-sm font-semibold border border-justice-blue/40 hover:bg-justice-blue/10 rounded transition-all text-slate-900 dark:text-white"
                        >
                            Lawyer Portal
                        </Link>
                        <Link
                            to="/user-login"
                            className="px-5 py-2 text-sm font-semibold bg-justice-blue text-white hover:bg-justice-blue/90 rounded shadow-lg shadow-justice-blue/20 transition-all text-center"
                        >
                            Client Login
                        </Link>
                    </div>
                </div>
            </nav>

            {/* Hero Section */}
            <section className="relative min-h-screen flex items-center pt-20 overflow-hidden">
                <div className="absolute inset-0 hero-glow -z-10"></div>
                <div className="max-w-7xl mx-auto px-6 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
                    <div className="space-y-8">
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full glass-primary border-justice-blue/20 text-justice-blue text-xs font-bold uppercase tracking-wider">
                            <span className="relative flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-justice-blue opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-justice-blue"></span>
                            </span>
                            Next-Gen Legal Platform
                        </div>
                        <h1 className="text-5xl lg:text-7xl font-extrabold leading-tight text-slate-900 dark:text-white">
                            Justice, <br />
                            <span className="text-justice-blue text-glow">Redefined by AI</span>
                        </h1>
                        <p className="text-lg text-slate-600 dark:text-slate-400 max-w-lg leading-relaxed">
                            Secure, transparent, and efficient legal solutions for the modern era. Empowering law firms and individuals with privacy-first legal intelligence.
                        </p>
                        <div className="flex flex-wrap gap-4 pt-4">
                            <Link
                                to="/user-register"
                                className="px-8 py-4 bg-justice-blue text-white font-bold rounded shadow-xl shadow-justice-blue/30 hover:scale-[1.02] transition-transform flex items-center gap-2"
                            >
                                Get Legal Help
                                <span className="material-icons text-sm">arrow_forward</span>
                            </Link>
                            <Link
                                to="/lawyer-login"
                                className="px-8 py-4 glass border-white/10 dark:border-white/5 font-bold rounded hover:bg-white/5 transition-colors text-slate-900 dark:text-white"
                            >
                                Join as Lawyer
                            </Link>
                        </div>
                        {/* Trust Section */}
                        <div className="flex items-center gap-4 pt-8">
                            <div className="px-4 py-2 glass rounded-full flex items-center gap-2 text-xs font-medium border-justice-blue/10">
                                <span className="material-icons text-justice-blue text-sm">verified_user</span>
                                ISO 27001 Certified
                            </div>
                            <div className="px-4 py-2 glass rounded-full flex items-center gap-2 text-xs font-medium border-justice-blue/10">
                                <span className="material-icons text-justice-blue text-sm">security</span>
                                GDPR Compliant
                            </div>
                            <div className="px-4 py-2 glass rounded-full flex items-center gap-2 text-xs font-medium border-justice-blue/10">
                                <span className="material-icons text-justice-blue text-sm">lock</span>
                                PII Masking Active
                            </div>
                        </div>
                    </div>
                    <div className="relative hidden lg:block">
                        <div className="relative z-10 w-full aspect-square glass rounded-2xl flex items-center justify-center p-8 border-justice-blue/20">
                            <img
                                alt="Digital Justice Scales Wireframe"
                                className="w-full h-full object-cover rounded-xl opacity-80"
                                src="https://lh3.googleusercontent.com/aida-public/AB6AXuDDZwp8pSH-pPOaZAhvZ3ipDY0k8qYs_fHaOWkxNVcSpkVluxmQkl3dmzpX42ZZYVYo-T3TgOrSpyFoVxFu3CJ_8fCsJ6P73lfrdmpjxMhN6yaxedfuX3Efgv5xNk_5_aQJ32hfnUaOyaYYsYtNF6dfSwqFDKDg8tyIv8_WSOiQXHMwAfsx6NRXRM0z46o3vlxq6lVHvGjKp0dLWQhgYZSPSwAKFT-HHAkLbMTAzDc0M7oGq4uOcNDjeaOsBuwiwmkbrgr-FyFAt0A"
                            />
                            {/* Floating Cards */}
                            <div className="absolute top-10 -left-10 glass-primary p-4 rounded-lg border-justice-blue/30 w-48 shadow-2xl">
                                <div className="flex items-center gap-3 mb-2">
                                    <div className="p-2 bg-justice-blue/20 rounded">
                                        <span className="material-icons text-justice-blue text-sm">analytics</span>
                                    </div>
                                    <span className="text-[10px] font-bold uppercase">Live Analysis</span>
                                </div>
                                <div className="h-1 w-full bg-justice-blue/20 rounded-full overflow-hidden">
                                    <div className="h-full bg-justice-blue w-2/3"></div>
                                </div>
                                <p className="text-[10px] mt-2 opacity-60">Processing 2.4k case nodes...</p>
                            </div>
                        </div>
                        {/* Background Decorative Blur */}
                        <div className="absolute -z-10 top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[120%] bg-justice-blue/10 blur-[100px] rounded-full"></div>
                    </div>
                </div>
            </section>

            {/* Features Section */}
            <section className="py-24 relative" id="solutions">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="text-center mb-16 space-y-4">
                        <h2 className="text-3xl lg:text-4xl font-extrabold text-slate-900 dark:text-white">Built for the Digital Future</h2>
                        <p className="text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
                            A comprehensive ecosystem designed to bridge the gap between complex legal processes and everyday efficiency.
                        </p>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                        {/* Card 1 */}
                        <div className="group p-8 rounded-xl glass hover:border-justice-blue/40 transition-all hover:-translate-y-1 relative overflow-hidden">
                            <div className="absolute -right-4 -top-4 w-24 h-24 bg-justice-blue/5 rounded-full blur-2xl group-hover:bg-justice-blue/10 transition-colors"></div>
                            <div className="w-14 h-14 bg-justice-blue/10 rounded flex items-center justify-center mb-6 group-hover:bg-justice-blue/20 transition-colors">
                                <span className="material-icons text-justice-blue text-3xl">psychology</span>
                            </div>
                            <h3 className="text-xl font-bold mb-4">AI Case Analysis</h3>
                            <p className="text-slate-600 dark:text-slate-400 text-sm leading-relaxed">
                                Instantly analyze thousands of legal documents to identify patterns, precedents, and risks with 99.8% accuracy.
                            </p>
                            <div className="mt-6 flex items-center gap-2 text-justice-blue font-bold text-xs">
                                LEARN MORE <span className="material-icons text-sm">chevron_right</span>
                            </div>
                        </div>
                        {/* Card 2 */}
                        <div className="group p-8 rounded-xl glass-primary hover:border-justice-blue/60 transition-all hover:-translate-y-1 relative overflow-hidden">
                            <div className="absolute -right-4 -top-4 w-24 h-24 bg-justice-blue/10 rounded-full blur-2xl group-hover:bg-justice-blue/20 transition-colors"></div>
                            <div className="w-14 h-14 bg-justice-blue rounded flex items-center justify-center mb-6 shadow-lg shadow-justice-blue/30">
                                <span className="material-icons text-white text-3xl">shield</span>
                            </div>
                            <h3 className="text-xl font-bold mb-4">Privacy-First PII Masking</h3>
                            <p className="text-slate-600 dark:text-slate-400 text-sm leading-relaxed">
                                Advanced redaction engines automatically detect and mask Personally Identifiable Information to ensure total compliance.
                            </p>
                            <div className="mt-6 flex items-center gap-2 text-justice-blue font-bold text-xs">
                                SECURITY PROTOCOLS <span className="material-icons text-sm">chevron_right</span>
                            </div>
                        </div>
                        {/* Card 3 */}
                        <div className="group p-8 rounded-xl glass hover:border-justice-blue/40 transition-all hover:-translate-y-1 relative overflow-hidden">
                            <div className="absolute -right-4 -top-4 w-24 h-24 bg-justice-blue/5 rounded-full blur-2xl group-hover:bg-justice-blue/10 transition-colors"></div>
                            <div className="w-14 h-14 bg-justice-blue/10 rounded flex items-center justify-center mb-6 group-hover:bg-justice-blue/20 transition-colors">
                                <span className="material-icons text-justice-blue text-3xl">translate</span>
                            </div>
                            <h3 className="text-xl font-bold mb-4">Bilingual Intelligence</h3>
                            <p className="text-slate-600 dark:text-slate-400 text-sm leading-relaxed">
                                Break language barriers with real-time translation and localized legal context support for global operations.
                            </p>
                            <div className="mt-6 flex items-center gap-2 text-justice-blue font-bold text-xs">
                                EXPLORE LANGUAGES <span className="material-icons text-sm">chevron_right</span>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Stats Section */}
            <section className="py-20 border-t border-white/5">
                <div className="max-w-7xl mx-auto px-6 grid grid-cols-2 md:grid-cols-4 gap-8">
                    <div className="text-center">
                        <div className="text-4xl font-extrabold text-justice-blue mb-2">500k+</div>
                        <div className="text-xs font-bold uppercase tracking-widest opacity-50">Cases Resolved</div>
                    </div>
                    <div className="text-center">
                        <div className="text-4xl font-extrabold text-justice-blue mb-2">12ms</div>
                        <div className="text-xs font-bold uppercase tracking-widest opacity-50">Processing Latency</div>
                    </div>
                    <div className="text-center">
                        <div className="text-4xl font-extrabold text-justice-blue mb-2">256-bit</div>
                        <div className="text-xs font-bold uppercase tracking-widest opacity-50">Encryption Std.</div>
                    </div>
                    <div className="text-center">
                        <div className="text-4xl font-extrabold text-justice-blue mb-2">95%</div>
                        <div className="text-xs font-bold uppercase tracking-widest opacity-50">Cost Efficiency</div>
                    </div>
                </div>
            </section>

            {/* Call to Action Section */}
            <section className="py-24">
                <div className="max-w-5xl mx-auto px-6">
                    <div className="relative glass rounded-2xl p-12 overflow-hidden text-center border-justice-blue/20">
                        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-justice-blue to-transparent"></div>
                        <div className="absolute -z-10 bottom-0 right-0 w-64 h-64 bg-justice-blue/5 blur-[80px] rounded-full"></div>
                        <h2 className="text-3xl font-extrabold mb-6">Ready to transform your legal workflow?</h2>
                        <p className="text-slate-600 dark:text-slate-400 mb-10 max-w-xl mx-auto">
                            Join thousands of law firms and legal professionals who are already using LegalConnect to automate the mundane and focus on the meaningful.
                        </p>
                        <div className="flex flex-col sm:flex-row justify-center gap-4">
                            <Link
                                to="/user-register"
                                className="px-8 py-4 bg-justice-blue text-white font-bold rounded hover:shadow-lg transition-shadow"
                            >
                                Start Your Free Trial
                            </Link>
                            <a
                                href="#contact"
                                className="px-8 py-4 glass border-white/10 font-bold rounded hover:bg-white/5 transition-colors text-slate-900 dark:text-white"
                            >
                                Request a Demo
                            </a>
                        </div>
                    </div>
                </div>
            </section>

            {/* Footer */}
            <footer className="py-12 border-t border-white/5 glass">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-12">
                        <div className="col-span-1 md:col-span-1">
                            <div className="flex items-center gap-2 mb-6">
                                <div className="w-8 h-8 bg-justice-blue rounded flex items-center justify-center">
                                    <span className="material-icons text-white text-sm">gavel</span>
                                </div>
                                <span className="text-lg font-extrabold text-slate-900 dark:text-white">
                                    Legal<span className="text-justice-blue">Connect</span>
                                </span>
                            </div>
                            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
                                Leading the digital transformation of justice through AI-driven intelligence and security.
                            </p>
                        </div>
                        <div>
                            <h4 className="font-bold mb-6 text-sm uppercase tracking-widest">Platform</h4>
                            <ul className="space-y-4 text-sm text-slate-600 dark:text-slate-400">
                                <li><a className="hover:text-justice-blue transition-colors" href="#">AI Document Review</a></li>
                                <li><a className="hover:text-justice-blue transition-colors" href="#">Case Management</a></li>
                                <li><a className="hover:text-justice-blue transition-colors" href="#">Client Intake</a></li>
                                <li><a className="hover:text-justice-blue transition-colors" href="#">Billing Automation</a></li>
                            </ul>
                        </div>
                        <div>
                            <h4 className="font-bold mb-6 text-sm uppercase tracking-widest">Company</h4>
                            <ul className="space-y-4 text-sm text-slate-600 dark:text-slate-400">
                                <li><a className="hover:text-justice-blue transition-colors" href="#">About Us</a></li>
                                <li><a className="hover:text-justice-blue transition-colors" href="#">Security Ethics</a></li>
                                <li><a className="hover:text-justice-blue transition-colors" href="#">Partners</a></li>
                                <li><a className="hover:text-justice-blue transition-colors" href="#">Careers</a></li>
                            </ul>
                        </div>
                        <div>
                            <h4 className="font-bold mb-6 text-sm uppercase tracking-widest">Connect</h4>
                            <div className="flex gap-4 mb-6">
                                <a className="w-10 h-10 glass rounded-full flex items-center justify-center hover:bg-justice-blue hover:text-white transition-all text-slate-900 dark:text-white" href="#">
                                    <span className="material-icons text-lg">public</span>
                                </a>
                                <a className="w-10 h-10 glass rounded-full flex items-center justify-center hover:bg-justice-blue hover:text-white transition-all text-slate-900 dark:text-white" href="#">
                                    <span className="material-icons text-lg">alternate_email</span>
                                </a>
                                <a className="w-10 h-10 glass rounded-full flex items-center justify-center hover:bg-justice-blue hover:text-white transition-all text-slate-900 dark:text-white" href="#">
                                    <span className="material-icons text-lg">share</span>
                                </a>
                            </div>
                            <div className="text-xs text-slate-500">
                                © 2024 LegalConnect Technologies Inc.<br />
                                All rights reserved.
                            </div>
                        </div>
                    </div>
                    <div className="pt-8 border-t border-white/5 text-center text-xs text-slate-500">
                        Built for Justice. Engineered for Security.
                    </div>
                </div>
            </footer>
        </div>
    );
}

export default LandingPage;
