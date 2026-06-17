import { useState, useEffect, useRef, useCallback } from 'react';
interface SpeechRecognitionEvent extends Event {
    resultIndex: number;
    results: SpeechRecognitionResultList;
}
interface SpeechRecognitionErrorEvent extends Event {
    error: string;
    message: string;
}
interface SpeechRecognition extends EventTarget {
    continuous: boolean;
    interimResults: boolean;
    lang: string;
    maxAlternatives: number;
    start: () => void;
    stop: () => void;
    abort: () => void;
    onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
    onresult: ((event: SpeechRecognitionEvent) => void) | null;
    onstart: (() => void) | null;
    onend: (() => void) | null;
}

interface SpeechRecognitionConstructor {
    new (): SpeechRecognition;
}

declare global {
    interface Window {
        SpeechRecognition?: SpeechRecognitionConstructor;
        webkitSpeechRecognition?: SpeechRecognitionConstructor;
    }
    interface Navigator {
        userLanguage?: string;
    }
}

interface UseSpeechRecognitionOptions {
    continuous?: boolean;
    interimResults?: boolean;
    lang?: string;
    onResult?: (transcript: string) => void;
    onError?: (error: string) => void;
}

interface UseSpeechRecognitionReturn {
    isListening: boolean;
    transcript: string;
    interimTranscript: string;
    startListening: () => void;
    stopListening: () => void;
    resetTranscript: () => void;
    isSupported: boolean;
    error: string | null;
}

// Get the system default language
const getSystemLanguage = (): string => {
    if (typeof window !== 'undefined' && window.navigator) {
        const userLang = navigator.language || navigator.userLanguage;
        return userLang || 'zh-CN';
    }
    return 'zh-CN';
};

export const useSpeechRecognition = (
    options: UseSpeechRecognitionOptions = {},
): UseSpeechRecognitionReturn => {
    const {
        lang = getSystemLanguage(),
        onResult,
        onError,
        continuous = true,
        interimResults = false,
    } = options;

    const [isListening, setIsListening] = useState(false);
    const [transcript, setTranscript] = useState('');
    const [interimTranscript, setInterimTranscript] = useState('');
    const [error, setError] = useState<string | null>(null);

    const recognitionRef = useRef<SpeechRecognition | null>(null);
    const isSupported =
        typeof window !== 'undefined' &&
        ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window);

    // Create a new recognition instance
    const createRecognitionInstance = useCallback(() => {
        if (!isSupported) return null;

        const SpeechRecognitionConstructor =
            window.SpeechRecognition || window.webkitSpeechRecognition;

        if (!SpeechRecognitionConstructor) return null;

        const recognition = new SpeechRecognitionConstructor();
        // Automatically stop recognizing when the voice ends.
        recognition.continuous = continuous;
        // Control whether to return temporary identification results
        recognition.interimResults = interimResults;
        recognition.lang = lang;
        recognition.onstart = () => {
            setIsListening(true);
        };
        // Handle recognition results
        recognition.onresult = (event: SpeechRecognitionEvent) => {
            let interimText = '';
            let finalText = '';

            for (let i = event.resultIndex; i < event.results.length; i++) {
                const result = event.results[i];
                const text = result[0].transcript;

                if (result.isFinal) {
                    finalText += text;
                } else {
                    interimText += text;
                }
            }

            if (finalText) {
                setTranscript((prev) => prev + finalText);
                if (onResult) {
                    onResult(finalText);
                }
            }

            setInterimTranscript(interimText);
        };

        // Handle errors
        recognition.onerror = (event: SpeechRecognitionErrorEvent) => {
            console.error('Speech recognition error:', event.error);
            let errorMessage = 'Voice recognition error';

            switch (event.error) {
                case 'no-speech':
                    errorMessage = 'No voice detected, please try again';
                    break;
                case 'audio-capture':
                    errorMessage = 'Cannot access microphone';
                    break;
                case 'not-allowed':
                    errorMessage = 'Microphone permission denied';
                    break;
                case 'network':
                    errorMessage = 'Network error';
                    break;
                case 'aborted':
                    return;
                default:
                    errorMessage = `Voice recognition error: ${event.error}`;
            }

            setError(errorMessage);
            setIsListening(false);

            if (onError && event.error !== 'aborted') {
                onError(errorMessage);
            }
        };

        // Recognition ended
        recognition.onend = () => {
            setIsListening(false);
            setInterimTranscript('');
        };

        return recognition;
    }, [lang, onResult, onError, isSupported]);

    // Clean up current recognition instance
    const cleanupRecognition = useCallback(() => {
        if (recognitionRef.current) {
            try {
                recognitionRef.current.onend = null;
                recognitionRef.current.onerror = null;
                recognitionRef.current.onresult = null;
                recognitionRef.current.stop();
            } catch {
                // Ignore errors when stopping
            }
            recognitionRef.current = null;
        }
    }, []);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            cleanupRecognition();
        };
    }, [cleanupRecognition]);

    const startListening = useCallback(() => {
        if (!isSupported) {
            setError('Voice recognition not available');
            return;
        }

        cleanupRecognition(); // Clean up old instance

        const recognition = createRecognitionInstance();
        if (!recognition) {
            setError('Unable to create voice recognition instance');
            return;
        }

        try {
            setError(null);
            recognitionRef.current = recognition;
            recognition.start();
            setIsListening(true);
        } catch (startError) {
            console.error('Failed to start recognition:', startError);
            setError('Failed to start voice recognition');
            setIsListening(false);
            recognitionRef.current = null;
        }
    }, [isSupported, cleanupRecognition, createRecognitionInstance]);

    const stopListening = useCallback(() => {
        if (recognitionRef.current) {
            try {
                // Set flag to avoid triggering error handling
                recognitionRef.current.onend = null;
                recognitionRef.current.stop();
            } catch (stopError) {
                console.error('Error stopping recognition:', stopError);
            } finally {
                setIsListening(false);
                setInterimTranscript('');
                recognitionRef.current = null;
            }
        }
    }, [setIsListening, setInterimTranscript]);

    const resetTranscript = useCallback(() => {
        setTranscript('');
        setInterimTranscript('');
        setError(null);
    }, []);

    return {
        isListening,
        transcript,
        interimTranscript,
        startListening,
        stopListening,
        resetTranscript,
        isSupported,
        error,
    };
};
