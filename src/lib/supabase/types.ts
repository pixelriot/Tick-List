export type Json =
	| string
	| number
	| boolean
	| null
	| { [key: string]: Json | undefined }
	| Json[];

export type Database = {
	public: {
		Tables: {
			lists: {
				Row: {
					id: string;
					name: string;
					created_at: string;
					updated_at: string;
					deleted_at: string | null;
				};
				Insert: {
					id: string;
					name: string;
					created_at?: string;
					updated_at?: string;
					deleted_at?: string | null;
				};
				Update: {
					id?: string;
					name?: string;
					created_at?: string;
					updated_at?: string;
					deleted_at?: string | null;
				};
				Relationships: [];
			};
			list_items: {
				Row: {
					id: string;
					list_id: string;
					name: string;
					checked: boolean;
					amount: number;
					comment: string | null;
					created_at: string;
					updated_at: string;
					deleted_at: string | null;
				};
				Insert: {
					id: string;
					list_id: string;
					name: string;
					checked?: boolean;
					amount?: number;
					comment?: string | null;
					created_at?: string;
					updated_at?: string;
					deleted_at?: string | null;
				};
				Update: {
					id?: string;
					list_id?: string;
					name?: string;
					checked?: boolean;
					amount?: number;
					comment?: string | null;
					created_at?: string;
					updated_at?: string;
					deleted_at?: string | null;
				};
				Relationships: [];
			};
			list_shares: {
				Row: {
					id: string;
					list_id: string;
					share_code: string;
					created_at: string;
					updated_at: string;
					deleted_at: string | null;
				};
				Insert: {
					id: string;
					list_id: string;
					share_code: string;
					created_at?: string;
					updated_at?: string;
					deleted_at?: string | null;
				};
				Update: {
					id?: string;
					list_id?: string;
					share_code?: string;
					created_at?: string;
					updated_at?: string;
					deleted_at?: string | null;
				};
				Relationships: [];
			};
		};
		Views: Record<string, never>;
		Functions: Record<string, never>;
		Enums: Record<string, never>;
		CompositeTypes: Record<string, never>;
	};
};
