export interface Document {
    id: number;
    filename: string;
    title: string;
    author: string;
    publicationDate: string;
    uploadDate: string;
    content?: string; // Full text content
    metadata?: Record<string, any>;
}
