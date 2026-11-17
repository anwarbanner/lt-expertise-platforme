import './configurationZod'
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './app/App'
// Polices embarquées dans l'application : aucun appel à un service externe (P11), fonctionne hors ligne
import '@fontsource/inter/latin-400.css'
import '@fontsource/inter/latin-500.css'
import '@fontsource/inter/latin-600.css'
import '@fontsource/inter/latin-700.css'
import '@fontsource/montserrat/latin-600.css'
import '@fontsource/montserrat/latin-700.css'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
