export interface Document {
    id: number;
    filename: string;
    title: string;
    author: string;
    publicationDate: string;
    uploadDate: string;
    content?: string; // Full text content
    collectionId?: number;
    metadata?: Record<string, any>;
}

export interface Collection {
    id: number;
    name: string;
    description: string;
    documents: Document[];
    createdDate: string;
}
