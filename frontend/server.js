const http = require('http');
const url = require('url');

const PORT = 3001;

// Sample task data
const tasks = [
  { id: 1, title: 'Design System Update', status: 'In Progress', dueDate: '2026-05-15', priority: 'High', description: 'Update the component library with new tokens' },
  { id: 2, title: 'API Integration', status: 'Completed', dueDate: '2026-04-25', priority: 'High', description: 'Integrate payment gateway API' },
  { id: 3, title: 'User Authentication', status: 'Completed', dueDate: '2026-04-20', priority: 'Medium', description: 'Implement OAuth2 authentication flow' },
  { id: 4, title: 'Mobile Responsiveness', status: 'In Progress', dueDate: '2026-05-10', priority: 'Medium', description: 'Ensure app works on all screen sizes' },
  { id: 5, title: 'Database Migration', status: 'Pending', dueDate: '2026-05-20', priority: 'High', description: 'Migrate from PostgreSQL to MongoDB' },
  { id: 6, title: 'Code Review', status: 'Overdue', dueDate: '2026-04-28', priority: 'Medium', description: 'Review pull requests from Q1' },
  { id: 7, title: 'Documentation', status: 'Pending', dueDate: '2026-06-01', priority: 'Low', description: 'Write API documentation for v2' },
  { id: 8, title: 'Performance Testing', status: 'In Progress', dueDate: '2026-05-25', priority: 'High', description: 'Load testing for production deployment' },
];

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const path = parsedUrl.pathname;

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  if (path === '/api/all' && req.method === 'GET') {
    // Simulate network delay
    setTimeout(() => {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(tasks));
    }, 300);
  } else if (path === '/api/all' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      try {
        const newTask = JSON.parse(body);
        newTask.id = tasks.length + 1;
        tasks.push(newTask);
        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(newTask));
      } catch {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Invalid JSON' }));
      }
    });
  } else {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not Found' }));
  }
});

server.listen(PORT, () => {
  console.log(`Mock API server running on http://localhost:${PORT}`);
  console.log(`GET /api/all - returns all tasks`);
  console.log(`POST /api/all - creates a new task`);
});
