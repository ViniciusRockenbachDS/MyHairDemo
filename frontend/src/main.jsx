import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { DEMO_LIGADA, Demonstracao } from './demo/Demonstracao.jsx';
import './styles/base.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      {DEMO_LIGADA ? (
        <Demonstracao>
          <App />
        </Demonstracao>
      ) : (
        <App />
      )}
    </BrowserRouter>
  </React.StrictMode>,
);
