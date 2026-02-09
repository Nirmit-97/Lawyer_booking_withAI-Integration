import React, { useState, useRef, useEffect } from 'react';
import { audioApi } from '../utils/api';
import { toast } from 'react-toastify';
import './AudioRecorder.css'; // We'll need to create this or assume styles are global/props

const AudioRecorder = ({ userId, onUploadSuccess }) => {
    const [isRecording, setIsRecording] = useState(false);
    const [recordingTime, setRecordingTime] = useState(0);
    const [audioBlob, setAudioBlob] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState('');
    const [audioUrl, setAudioUrl] = useState(null);

    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);
    const timerRef = useRef(null);

    useEffect(() => {
        return () => {
            if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
                mediaRecorderRef.current.stop();
            }
            if (timerRef.current) {
                clearInterval(timerRef.current);
            }
        };
    }, []);

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

            const supportedTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
            let selectedMimeType = 'audio/webm';
            for (const type of supportedTypes) {
                if (MediaRecorder.isTypeSupported(type)) {
                    selectedMimeType = type;
                    break;
                }
            }

            const mediaRecorder = new MediaRecorder(stream, { mimeType: selectedMimeType });
            mediaRecorderRef.current = mediaRecorder;
            audioChunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) audioChunksRef.current.push(event.data);
            };

            mediaRecorder.onstop = () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
                setAudioBlob(audioBlob);
                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorder.start();
            setIsRecording(true);
            setRecordingTime(0);
            setError('');
            setResult(null);

            timerRef.current = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);
        } catch (err) {
            setError('Error accessing microphone: ' + err.message);
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
            clearInterval(timerRef.current);
        }
    };

    const handleUpload = async () => {
        if (!audioBlob || !userId) return;

        setIsUploading(true);
        setError('');
        setResult(null);

        const formData = new FormData();
        const fileName = audioBlob.name || 'recording.webm';
        formData.append('file', audioBlob, fileName);
        formData.append('userId', userId.toString());

        try {
            const response = await audioApi.upload(formData, userId);
            const data = response.data;
            setResult(data);

            if (data.maskedTextAudioBase64) {
                const base64String = data.maskedTextAudioBase64.replace(/^data:audio\/\w+;base64,/, '');
                setAudioUrl(`data:audio/mpeg;base64,${base64String}`);
            }

            if (data.caseId) {
                toast.success('Audio processed and Case created!');
            } else {
                toast.error('Audio uploaded, but automatic case creation failed. Please check My Cases or contact support.');
            }
            if (onUploadSuccess) {
                onUploadSuccess(data);
            }

        } catch (err) {
            console.error('Upload Error', err);
            setError('Upload failed: ' + (err.response?.data?.error || err.message));
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <div className="audio-recorder-container">
            <div className="recording-section">
                <h2>Record Audio</h2>
                <div className="recording-controls">
                    {!isRecording ? (
                        <button onClick={startRecording} className="record-button start">
                            <span className="record-icon">🎤</span> Start Recording
                        </button>
                    ) : (
                        <button onClick={stopRecording} className="record-button stop">
                            <span className="record-icon">⏹</span> Stop Recording ({formatTime(recordingTime)})
                        </button>
                    )}
                </div>

                {audioBlob && (
                    <div className="audio-preview">
                        <p>Audio recorded! ({Math.round(audioBlob.size / 1024)} KB)</p>
                        <button onClick={handleUpload} disabled={isUploading} className="upload-button">
                            {isUploading ? 'Processing...' : 'Upload & Process Audio'}
                        </button>
                        <button onClick={() => setAudioBlob(null)} className="reset-recording-button" style={{ marginLeft: '10px' }}>
                            Cancel
                        </button>
                    </div>
                )}

                {/* File Upload Input */}
                <div className="file-upload-section" style={{ marginTop: '20px', padding: '15px', background: '#f8f9fa' }}>
                    <h3>Or Upload File</h3>
                    <input type="file" accept="audio/*" onChange={(e) => setAudioBlob(e.target.files[0])} />
                </div>

                {error && <div className="error-message" style={{ color: 'red', marginTop: '10px' }}>{error}</div>}

                {result && (
                    <div className="results-section">
                        <h2>Processing Results</h2>
                        <div className="result-card">
                            <h3>Original Text</h3>
                            <div className="text-content">{result.originalEnglishText}</div>
                        </div>
                        <div className="result-card">
                            <h3>Masked Text</h3>
                            <div className="text-content">{result.maskedEnglishText}</div>
                        </div>
                        {audioUrl && (
                            <div className="result-card">
                                <h3>Masked Audio</h3>
                                <audio controls src={audioUrl} />
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default AudioRecorder;
