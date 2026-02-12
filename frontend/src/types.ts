export interface Document {
    id: number;
    filename: string;
    title: string;
    author: string;
    publicationDate: string;
    uploadDate: string;
    collectionId?: number;
}

export interface Collection {
    id: number;
    name: string;
    description: string;
    documents: Document[];
    createdDate: string;
}
