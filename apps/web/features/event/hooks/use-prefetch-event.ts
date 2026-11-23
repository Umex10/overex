"use client";

import { useEffect, useState } from "react";
import { usePrefetch } from "../event-api";

export function usePrefetchEvent(eventId: string) {

      const [isHovering, setIsHovering] = useState(false);
      const prefetch = usePrefetch("getEvent");
    
      useEffect(() => {
    
        if (!isHovering) {
          return;
        }
    
        const timeout = setTimeout(() => {
          prefetch(eventId);
    
        }, 200)
    
        return () => {
          clearTimeout(timeout);
        }
    
      }, [isHovering, prefetch, eventId])


    return {
        onMouseEnter: () => setIsHovering(true),
        onMouseLeave: () => setIsHovering(false),
        onFocus: () => setIsHovering(true)    
    }

}
