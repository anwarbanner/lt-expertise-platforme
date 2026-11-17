// Validation des formulaires sans eval : compatible avec la politique de sécurité du contenu (script-src 'self').
// Module importé en premier par main.tsx : la règle doit précéder la création des schémas zod des écrans.
import { z } from 'zod'

z.config({ jitless: true })
