import { Schema, model } from 'mongoose';

export interface UserDoc {
  uid: string;
  email: string;
  name: string;
  preferences?: Record<string, unknown>;
  createdAt: Date;
  updatedAt: Date;
}

const userSchema = new Schema<UserDoc>(
  {
    uid: { type: String, required: true, unique: true, index: true },
    email: { type: String, required: true, index: true },
    name: { type: String, required: true },
    preferences: { type: Schema.Types.Mixed }
  },
  { timestamps: true }
);


export const UserModel = model<UserDoc>('User', userSchema);



