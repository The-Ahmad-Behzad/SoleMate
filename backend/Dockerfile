# Build stage
FROM node:22-alpine AS builder

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
COPY package*.json ./

# Install all dependencies (including devDependencies for building)
RUN npm ci

# Bundle app source
COPY . .

# Build the TypeScript code
RUN npm run build

# Production stage
FROM node:22-alpine

# Create app directory
WORKDIR /usr/src/app

# Set environment to production
ENV NODE_ENV=production
ENV PORT=8080

# Install only production dependencies
COPY package*.json ./
RUN npm ci --omit=dev

# Copy built artifacts from the builder stage
COPY --from=builder /usr/src/app/dist ./dist

# Expose the API port
EXPOSE 8080

# Start the application
CMD ["npm", "start"]
