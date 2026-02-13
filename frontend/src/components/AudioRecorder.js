import React, { useState, useRef, useEffect } from 'react';
import { audioApi } from '../utils/api';
import { toast } from 'react-toastify';

const AudioRecorder = ({ userId, onUploadSuccess }) => {
    const [isRecording, setIsRecording] = useState(false);
    const [recordingTime, setRecordingTime] = useState(0);
    const [audioBlob, setAudioBlob] = useState(null);
    const [uploadedFile, setUploadedFile] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState('');
    const [audioUrl, setAudioUrl] = useState(null);

    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);
    const timerRef = useRef(null);
    const fileInputRef = useRef(null);

    useEffect(() => {
        return () => {
            if (mediaRecorderRef.current?.state !== 'inactive') mediaRecorderRef.current?.stop();
            if (timerRef.current) clearInterval(timerRef.current);
        };
    }, []);

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const startRecording = async () => {
        try {
            setUploadedFile(null); // Clear uploaded file if starting new recording
            setAudioBlob(null);
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream);
            mediaRecorderRef.current = mediaRecorder;
            audioChunksRef.current = [];

            mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunksRef.current.push(e.data); };
            mediaRecorder.onstop = () => {
                const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
                setAudioBlob(blob);
                stream.getTracks().forEach(t => t.stop());
            };

            mediaRecorder.start();
            setIsRecording(true);
            setRecordingTime(0);
            setError('');
            setResult(null);
            timerRef.current = setInterval(() => setRecordingTime(prev => prev + 1), 1000);
        } catch (err) { setError('Microphone access denied: ' + err.message); }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current?.state !== 'inactive') {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
            clearInterval(timerRef.current);
        }
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 25 * 1024 * 1024) { // 25MB limit
                setError('File size too large. Max 25MB.');
                return;
            }
            setUploadedFile(file);
            setAudioBlob(null); // Clear recorded blob
            setError('');
            setResult(null);
            setIsRecording(false);
        }
    };

    const handleUpload = async () => {
        const fileToUpload = uploadedFile || audioBlob;
        if (!fileToUpload || !userId) return;

        setIsUploading(true);
        setError('');
        const formData = new FormData();
        const filename = uploadedFile ? uploadedFile.name : 'recording.webm';
        formData.append('file', fileToUpload, filename);
        formData.append('userId', userId.toString());

        try {
            const response = await audioApi.upload(formData, userId);
            const data = response.data;
            setResult(data);
            if (data.maskedTextAudioBase64) {
                setAudioUrl(`data:audio/mpeg;base64,${data.maskedTextAudioBase64.replace(/^data:audio\/\w+;base64,/, '')}`);
            }
            toast.success('Intelligence gathered successfully');
            if (onUploadSuccess) onUploadSuccess(data);
        } catch (err) { setError('Transmission failed.'); }
        finally { setIsUploading(false); }
    };

    return (
        <div className="w-full space-y-6">
            <div className="flex flex-col items-center justify-center p-8 bg-white/5 backdrop-blur-xl rounded-[2.5rem] border border-white/10 shadow-2xl relative overflow-hidden">
                <div className="absolute top-0 left-0 w-full h-1 bg-white/10 overflow-hidden">
                    {isRecording && <div className="h-full bg-red-500 animate-pulse w-full origin-left" style={{ animationDuration: '2s' }}></div>}
                </div>

                <div className="flex items-center gap-8">
                    {!isRecording ? (
                        <div className="flex items-center gap-4">
                            <button
                                onClick={startRecording}
                                className="w-16 h-16 bg-white text-primary rounded-full flex items-center justify-center hover:scale-110 active:scale-95 transition-all shadow-xl shadow-white/10 group"
                                title="Start Recording"
                            >
                                <span className="material-symbols-outlined !text-3xl group-hover:animate-pulse">mic</span>
                            </button>

                            <div className="w-[1px] h-10 bg-white/20"></div>

                            <input
                                type="file"
                                ref={fileInputRef}
                                onChange={handleFileChange}
                                accept="audio/*"
                                className="hidden"
                            />
                            <button
                                onClick={() => fileInputRef.current.click()}
                                className="w-12 h-12 bg-white/10 text-white rounded-full flex items-center justify-center hover:bg-white/20 transition-all border border-white/10"
                                title="Upload Audio File"
                            >
                                <span className="material-symbols-outlined !text-2xl">upload_file</span>
                            </button>
                        </div>
                    ) : (
                        <button
                            onClick={stopRecording}
                            className="w-16 h-16 bg-red-500 text-white rounded-full flex items-center justify-center hover:scale-110 active:scale-95 transition-all shadow-xl shadow-red-500/20"
                        >
                            <span className="material-symbols-outlined !text-3xl">stop</span>
                        </button>
                    )}

                    <div className="flex flex-col">
                        <span className="text-[10px] font-black uppercase tracking-[0.3em] text-white/50 mb-1">
                            {isRecording ? 'Capturing Session' : uploadedFile ? 'File Selected' : 'Ready to record'}
                        </span>
                        <span className={`text-xl md:text-3xl font-black text-white ${isRecording ? 'animate-pulse' : ''} truncate max-w-[200px]`}>
                            {isRecording ? formatTime(recordingTime) : uploadedFile ? uploadedFile.name : '00:00'}
                        </span>
                    </div>
                </div>

                {(audioBlob || uploadedFile) && !isRecording && (
                    <div className="mt-8 flex items-center gap-4 animate-in fade-in slide-in-from-top-4">
                        <button
                            onClick={handleUpload}
                            disabled={isUploading}
                            className="px-8 py-3 bg-electric-blue text-white rounded-2xl text-[10px] font-black uppercase tracking-widest hover:bg-blue-600 transition-all shadow-lg shadow-blue-500/20 disabled:grayscale"
                        >
                            {isUploading ? 'Analyzing...' : 'Process Intelligence'}
                        </button>
                        <button onClick={() => { setAudioBlob(null); setUploadedFile(null); }} className="p-3 text-white/40 hover:text-white transition-colors">
                            <span className="material-symbols-outlined">delete</span>
                        </button>
                    </div>
                )}
            </div>

            {/* AI Results Section */}
            {result && (
                <div className="animate-in fade-in slide-in-from-bottom-8 duration-700">
                    <div className="bg-white dark:bg-background-dark/30 rounded-[2.5rem] border border-gray-100 dark:border-gray-800 p-8 shadow-sm">
                        <div className="flex justify-between items-center mb-8 pb-6 border-b border-gray-100 dark:border-gray-800">
                            <h3 className="text-xl font-black tracking-tight">Cognitive Analysis</h3>
                            <div className="flex items-center gap-2 px-4 py-1.5 bg-emerald-500/10 text-emerald-500 rounded-full border border-emerald-500/20 shadow-sm shadow-emerald-500/5">
                                <span className="material-symbols-outlined !text-[12px]">security</span>
                                <span className="text-[9px] font-black uppercase tracking-widest">PII Shield Engaged</span>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                            <div className="space-y-3">
                                <span className="text-[10px] font-black uppercase tracking-widest text-gray-400 px-1">Raw Input Transcription</span>
                                <div className="p-6 bg-gray-50 dark:bg-gray-900 rounded-3xl text-sm font-medium leading-relaxed italic opacity-70">
                                    "{result.originalEnglishText}"
                                </div>
                            </div>
                            <div className="space-y-3">
                                <span className="text-[10px] font-black uppercase tracking-widest text-emerald-500 px-1">Sanitized Output Archive</span>
                                <div className="p-6 bg-emerald-500/5 dark:bg-emerald-500/10 rounded-3xl border border-emerald-500/10 text-sm font-black text-primary dark:text-white leading-relaxed">
                                    {result.maskedEnglishText}
                                </div>
                            </div>
                        </div>

                        {audioUrl && (
                            <div className="mt-8 pt-8 border-t border-gray-100 dark:border-gray-800 text-center">
                                <span className="text-[10px] font-black uppercase tracking-widest text-electric-blue block mb-4">Reconstructed Secure Audio</span>
                                <audio controls src={audioUrl} className="w-full max-w-lg mx-auto h-10 grayscale opacity-70 hover:opacity-100 invert transition-all" />
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default AudioRecorder;
