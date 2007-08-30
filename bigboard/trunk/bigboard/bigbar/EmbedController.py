import deskbar.interfaces.Controller

class EmbedController(deskbar.interfaces.Controller):
    
    def __init__(self, model):
        deskbar.interfaces.Controller.__init__(self, model)
    
    def on_quit(self, *args):
        pass
    
    def on_query_entry_changed(self, entry):
        self._view.clear_results()
        self._view.clear_actions()
        self._model.stop_queries()
        # TODO: abort previous searches
        qstring = entry.get_text().strip()
        if (qstring != ""):
            self._view.show_results()
            self._model.query( qstring )
        else:
            self._view.clear_all()
    
    def on_query_entry_key_press_event(self, entry, event):
        raise NotImplementedError
    
    def on_query_entry_activate(self, entry):
        path, column = self._view.cview.get_cursor ()
        model = self._view.cview.get_model()
        iter = None
        if path != None:
            iter = model.get_iter (path)
        
        if iter == None or model.iter_has_child(iter):
            # No selection, select top element
            iter = model.get_iter_first()
            
            while iter != None and (not (model.iter_has_child(iter) and self._view.cview.row_expanded(model.get_path(iter))) ):
                iter = model.iter_next(iter)
            if iter != None:
                iter = model.iter_children(iter)

        if iter is None:
            return
        # retrieve new path-col pair, since it might have been None to start out
        path = model.get_path (iter)
        
        column = self._view.cview.get_column (0)
        # This emits a match-selected from the cview
        self._view.cview.emit ("row-activated", path, column)
        
    def on_treeview_cursor_changed(self, treeview):
        self._view.update_entry_icon ()
        
    def on_match_selected(self, treeview, text, match_obj, event):
        if len(match_obj.get_actions()) == 1:
            action = match_obj.get_actions()[0]
            self.on_action_selected(None, text, action, event)
        elif len(match_obj.get_actions()) > 1:
            self._view.display_actions(match_obj.get_actions(), text)
        else:
            raise Exception("Match has no action")
     
    def on_do_default_action(self, treeview, text, match_obj, event):
        action = match_obj.get_default_action()
        if action == None:
            action = match_obj.get_actions()[0]
        self.on_action_selected(treeview, text, action, event)
        
    def on_action_selected(self, treeview, text, action, event):
        action.activate(text)
    