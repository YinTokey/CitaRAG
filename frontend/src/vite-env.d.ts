/// <reference types="vite/client" />

declare module '*?url' {
    const fileUrlContent: string;
    export default fileUrlContent;
}

declare module 'pdfjs-dist/build/pdf.worker.min.mjs' {
    const workerSrc: string;
    export default workerSrc;
}
